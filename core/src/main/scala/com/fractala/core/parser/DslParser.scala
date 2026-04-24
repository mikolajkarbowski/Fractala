package com.fractala.core.parser

import fastparse.*
import fastparse.ScalaWhitespace.*
import fastparse.internal.Util
import com.fractala.core.models.{Color, Config, Rule, Symbol}
import com.fractala.core.traits.Parser
import com.fractala.core.parser.ast.{
  AstAxiomBlock,
  AstBlock,
  AstColorField,
  AstColorReq,
  AstColorsBlock,
  AstConfigBlock,
  AstConfigField,
  AstDoubleField,
  AstIntField,
  AstRule,
  AstRulesBlock,
  AstStandard,
  AstSymbol
}

/** A concrete implementation of the [[Parser]] trait using the FastParse library. * This object provides the actual
  * parsing logic to convert a custom L-System Domain Specific Language (DSL) into domain models. It is designed to be
  * highly resilient and user-friendly, supporting:
  *   - Implicit whitespace and newline handling.
  *   - Single-line comments (using `//`) and multi-line comments (using `/* ... */`).
  *   - Case-insensitive configuration keys.
  *   - Automatic fallback to default [[Config]] values for omitted fields.
  */
object DslParser extends Parser {

  private def number[$: P]: P[Double] = P(
    CharIn("0-9").repX(1) ~~ ("." ~~ CharIn("0-9").repX(1)).?
  ).!.map(_.toDouble)

  private def integer[$: P]: P[Int] = P(
    CharIn("0-9").repX(1).!
  ).map(_.toInt)

  private def identifier[$: P]: P[String] = P(CharIn("a-zA-Z").repX(1).!)

  private def doubleField[$: P](name: String): P[AstConfigField] =
    P(IgnoreCase(name) ~ ":" ~/ number).map(v => AstDoubleField(name.toLowerCase, v))

  private def intField[$: P](name: String): P[AstConfigField] =
    P(IgnoreCase(name) ~ ":" ~/ integer).map(v => AstIntField(name.toLowerCase, v))

  private def colorField[$: P](name: String): P[AstConfigField] =
    P(IgnoreCase(name) ~ ":" ~/ Index ~ identifier).map((idx, color) => AstColorField(name.toLowerCase, color, idx))

  private def configField[$: P]: P[AstConfigField] = P(
    doubleField("lineLength") | doubleField("lineWidth") | doubleField("turningAngle") |
      doubleField("lineWidthIncrement") | doubleField("lineLengthMultiplier") | doubleField("turningAngleIncrement") |
      colorField("startingColor") | intField("maxIterations")
  )

  private def configBlock[$: P]: P[AstBlock] = P(IgnoreCase("Config") ~ "{" ~/ configField.rep ~ "}")
    .filter { fields =>
      fields.map(f => f.fieldName).distinct.size == fields.size
    }
    .map { fields => AstConfigBlock(fields.toList) }

  private def colorChangeSymbol[$: P]: P[AstColorReq] = P("<" ~ Index ~ identifier ~ ">").map { case (idx, colorName) =>
    AstColorReq(colorName.toLowerCase, idx)
  }

  private def otherSymbol[$: P]: P[AstStandard] = P(CharPred(c => !c.isWhitespace && c != '{' && c != '}').!)
    .filter(str => Symbol.from(str.head).isDefined)
    .map(str => AstStandard(Symbol.from(str.head).get))

  private def lSystemSymbol[$: P]: P[AstSymbol] = P(colorChangeSymbol | otherSymbol)

  private def inlineSpace[$: P] = P(CharIn(" \t").repX)

  private def symbolList[$: P]: P[List[AstSymbol]] = P(
    (lSystemSymbol ~~ inlineSpace).repX(1)
  ).map(_.toList)

  private def axiomBlock[$: P]: P[AstBlock] = P(IgnoreCase("Axiom") ~ ":" ~/ symbolList).map(AstAxiomBlock.apply)

  private def ruleWeight[$: P]: P[Double] = P("(" ~ number ~ ")")

  private def rule[$: P]: P[AstRule] = P(
    otherSymbol ~ ruleWeight.? ~ ("->" | "→") ~ symbolList
  ).map { case (lhs, weightOpt, rhs) =>
    val rule = AstRule(
      predecessor = lhs.symbol,
      successor = rhs
    )

    weightOpt match {
      case Some(w) => rule.copy(weight = w)
      case None    => rule
    }
  }

  private def rulesBlock[$: P]: P[AstBlock] = P(IgnoreCase("Rules") ~ "{" ~/ rule.rep ~ "}").map { v =>
    AstRulesBlock(v.toList)
  }

  private def colorDefValue[$: P]: P[Color] = P(number ~ "," ~/ number ~ "," ~/ number).map { case (r, g, b) =>
    Color(r, g, b)
  }

  private def colorDef[$: P]: P[(String, Color)] = P(identifier ~ ":" ~/ colorDefValue).map { case (name, c) =>
    (name.toLowerCase, c)
  }

  private def colorsBlock[$: P]: P[AstBlock] = P(IgnoreCase("Colors") ~ "{" ~/ colorDef.rep ~ "}").map { fields =>
    AstColorsBlock(fields.toMap)
  }

  private def dslSystem[$: P]: P[List[AstBlock]] = P(
    Start ~ (configBlock | colorsBlock | axiomBlock | rulesBlock).rep ~ End
  ).map(_.toList)

  private def createParseError(input: String, msg: String, index: Int): ParseError = {
    val lines = input.substring(0, index).split("\n", -1)
    ParseError(msg, lines.length, lines.last.length + 1, index)
  }

  private def resolveAstSymbol(ast: AstSymbol, palette: Map[String, Color], input: String): Either[ParseError, Symbol] =
    ast match {
      case AstStandard(sym) => Right(sym)
      case AstColorReq(name, idx) =>
        palette.get(name) match {
          case Some(c) => Right(Symbol.ColorChange(c))
          case None    => Left(createParseError(input, s"Undefined color '$name' used in standalone parsing.", idx))
        }
    }

  private def resolveDsl(blocks: List[AstBlock], input: String): Either[ParseError, DslResult] = {
    val palette = blocks.collect { case AstColorsBlock(palette) => palette }.flatten.toMap

    def resolveSymbol(ast: AstSymbol): Either[ParseError, Symbol] = ast match {
      case AstStandard(sym) => Right(sym)
      case AstColorReq(name, idx) =>
        palette.get(name) match {
          case Some(c) => Right(Symbol.ColorChange(c))
          case None    => Left(createParseError(input, s"Undefined color '$name' used in symbol.", idx))
        }
    }

    val configFields = blocks.collect { case AstConfigBlock(fields) => fields }.flatten
    val configResult = configFields.foldLeft[Either[ParseError, Config]](Right(Config())) {
      case (Left(err), _)                                           => Left(err)
      case (Right(cfg), AstDoubleField("linelength", v))            => Right(cfg.copy(lineLength = v))
      case (Right(cfg), AstDoubleField("linewidth", v))             => Right(cfg.copy(lineWidth = v))
      case (Right(cfg), AstDoubleField("turningangle", v))          => Right(cfg.copy(turningAngle = v))
      case (Right(cfg), AstDoubleField("linewidthincrement", v))    => Right(cfg.copy(lineWidthIncrement = v))
      case (Right(cfg), AstDoubleField("linelengthmultiplier", v))  => Right(cfg.copy(lineLengthMultiplier = v))
      case (Right(cfg), AstDoubleField("turningangleincrement", v)) => Right(cfg.copy(turningAngleIncrement = v))
      case (Right(cfg), AstIntField("maxiterations", v))            => Right(cfg.copy(maxIterations = v))

      case (Right(cfg), AstColorField(_, colorName, idx)) =>
        palette.get(colorName) match {
          case Some(c) => Right(cfg.copy(startingColor = c))
          case None    => Left(createParseError(input, s"Undefined color '$colorName' in Config.", idx))
        }
      case (Right(cfg), _) => Right(cfg)
    }

    val rawAxiom = blocks.collect { case AstAxiomBlock(symbols) => symbols }.flatten
    val axiomResult = rawAxiom.foldLeft[Either[ParseError, List[Symbol]]](Right(List.empty)) {
      case (Left(err), _)       => Left(err)
      case (Right(acc), astSym) => resolveSymbol(astSym).map(sym => acc :+ sym)
    }

    val rawRules = blocks.collect { case AstRulesBlock(rules) => rules }.flatten
    val rulesResult = rawRules.foldLeft[Either[ParseError, List[Rule]]](Right(List.empty)) {
      case (Left(err), _) => Left(err)
      case (Right(acc), AstRule(predecessor, weightOpt, rawSuccessor)) =>
        val resolvedSuccessors = rawSuccessor.foldLeft[Either[ParseError, List[Symbol]]](Right(List.empty)) {
          case (Left(e), _)                   => Left(e)
          case (Right(successorsAcc), astSym) => resolveSymbol(astSym).map(s => successorsAcc :+ s)
        }
        resolvedSuccessors.map(s => acc :+ Rule(predecessor, weightOpt, s))
    }

    for {
      config <- configResult
      axiom <- axiomResult
      rules <- rulesResult
    } yield DslResult(config, axiom, rules)
  }

  def parseDsl(input: String): Either[ParseError, DslResult] = {
    parse(input, dslSystem(_)) match {
      case Parsed.Success(blocks, _) =>
        resolveDsl(blocks, input)

      case failure: Parsed.Failure =>
        Left(
          createParseError(
            input,
            s"Syntax Error: Expected ${failure.trace().aggregateMsg}",
            failure.index
          )
        )
    }
  }

  def parseSymbols(input: String, palette: Map[String, Color]): Either[ParseError, List[Symbol]] = {
    def symbolsEntry[$: P]: P[List[AstSymbol]] = P(Start ~ symbolList ~ End)

    parse(input, symbolsEntry(_)) match {
      case Parsed.Success(astList, _) =>
        astList.foldLeft[Either[ParseError, List[Symbol]]](Right(List.empty)) {
          case (Left(e), _)      => Left(e)
          case (Right(acc), ast) => resolveAstSymbol(ast, palette, input).map(sym => acc :+ sym)
        }
      case failure: Parsed.Failure =>
        Left(createParseError(input, s"Expected ${failure.trace().aggregateMsg}", failure.index))
    }
  }

  def parseRule(input: String, palette: Map[String, Color]): Either[ParseError, Rule] = {
    def ruleEntry[$: P]: P[AstRule] = P(Start ~ rule ~ End)

    parse(input, ruleEntry(_)) match {
      case Parsed.Success(astRule, _) =>
        val resolvedSuccessors = astRule.successor.foldLeft[Either[ParseError, List[Symbol]]](Right(List.empty)) {
          case (Left(e), _)         => Left(e)
          case (Right(acc), astSym) => resolveAstSymbol(astSym, palette, input).map(s => acc :+ s)
        }
        resolvedSuccessors.map(successor => Rule(astRule.predecessor, astRule.weight, successor))

      case failure: Parsed.Failure =>
        Left(createParseError(input, s"Expected ${failure.trace().aggregateMsg}", failure.index))
    }
  }
}
