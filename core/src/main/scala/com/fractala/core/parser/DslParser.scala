package com.fractala.core.parser

import fastparse.*
import fastparse.ScalaWhitespace._
import com.fractala.core.models.{Color, Config, Rule, Symbol}
import com.fractala.core.traits.Parser

/**
 * A concrete implementation of the [[Parser]] trait using the FastParse library.
 * * This object provides the actual parsing logic to convert a custom L-System Domain Specific Language (DSL)
 * into domain models. It is designed to be highly resilient and user-friendly, supporting:
 * - Implicit whitespace and newline handling.
 * - Single-line comments (using `//`) and multi-line comments (using `/* ... */`).
 * - Case-insensitive configuration keys.
 * - Automatic fallback to default [[Config]] values for omitted fields.
 */
object DslParser extends Parser {

  private def number[$: P]: P[Double] = P(
    CharIn("0-9").repX(1) ~~ ("." ~~ CharIn("0-9").repX(1)).?
  ).!.map(_.toDouble)

  private def colorString[$: P]: P[Color] = P(CharIn("a-zA-Z").repX(1).!)
    .filter(c => Color.fromString(c).isDefined)
    .map(c => Color.fromString(c).get)

  private def doubleField[$: P](name: String): P[(String, Double)] = P(IgnoreCase(name) ~ ":" ~/ number).map(v => (name.toLowerCase, v))

  private def colorField[$: P](name: String): P[(String, Color)] = P(IgnoreCase(name) ~ ":" ~/ colorString).map(v => (name.toLowerCase, v))

  private def configField[$: P]: P[(String, Any)] = P(
    doubleField("lineLength") | doubleField("lineWidth") | doubleField("turningAngle") |
      doubleField("lineWidthIncrement") | doubleField("lineLengthMultiplier") | doubleField("turningAngleIncrement") |
      colorField("startingColor")
  )

  private def configBlock[$: P]: P[Config] = P(IgnoreCase("Config") ~ "{" ~/ configField.rep ~ "}").map { fields =>
    val map = fields.toMap
    val default = Config()
    Config(
      lineLength = map.getOrElse("lineLength".toLowerCase, default.lineLength).asInstanceOf[Double],
      lineWidth = map.getOrElse("lineWidth".toLowerCase, default.lineWidth).asInstanceOf[Double],
      turningAngle = map.getOrElse("turningAngle".toLowerCase, default.turningAngle).asInstanceOf[Double],
      lineWidthIncrement = map.getOrElse("lineWidthIncrement".toLowerCase, default.lineWidthIncrement).asInstanceOf[Double],
      lineLengthMultiplier = map.getOrElse("lineLengthMultiplier".toLowerCase, default.lineLengthMultiplier).asInstanceOf[Double],
      turningAngleIncrement = map.getOrElse("turningAngleIncrement".toLowerCase, default.turningAngleIncrement).asInstanceOf[Double],
      startingColor = map.getOrElse("startingColor".toLowerCase, default.startingColor).asInstanceOf[Color]
    )
  }

  private def colorChangeSymbol[$: P]: P[Symbol.ColorChange] = P("<" ~ colorString ~ ">").map(Symbol.ColorChange.apply)

  private def otherSymbol[$: P]: P[Symbol] = P(CharPred(c => !c.isWhitespace && c != '{' && c != '}').!)
    .filter(str => Symbol.fromChar(str.head).isDefined)
    .map(str => Symbol.fromChar(str.head).get)

  private def lSystemSymbol[$: P]: P[Symbol] = P(colorChangeSymbol | otherSymbol)

  private def inlineSpace[$: P] = P(CharIn(" \t").repX)

  private def symbolList[$: P]: P[List[Symbol]] = P(
    (lSystemSymbol ~~ inlineSpace).repX(1)
  ).map(_.toList)

  private def axiomBlock[$: P]: P[List[Symbol]] = P(IgnoreCase("Axiom") ~ ":" ~/ symbolList)

  private def ruleWeight[$: P]: P[Double] = P("(" ~ number ~ ")")

  private def rule[$: P]: P[Rule] = P(
    otherSymbol ~ ruleWeight.? ~ ("->" | "→") ~ symbolList
  ).map { case (lhs, weightOpt, rhs) =>
    val rule = Rule(
      predecessor = lhs,
      successor = rhs,
    )

    weightOpt match {
      case Some(w) => rule.copy(weight = w)
      case None => rule
    }
  }

  private def rulesBlock[$: P]: P[List[Rule]] = P(IgnoreCase("Rules") ~ "{" ~/ rule.rep ~ "}").map(_.toList)

  private def dslSystem[$: P]: P[DslResult] = P(Start ~ configBlock.? ~ axiomBlock ~ rulesBlock ~ End).map {
    case (configOpt, axiom, rules) =>
      DslResult(configOpt.getOrElse(Config()), axiom, rules)
  }

  def parseDSL(input: String): Either[String, DslResult] = {
    parse(input, dslSystem(_)) match {
      case Parsed.Success(result, _) => Right(result)
      case failure: Parsed.Failure => Left(failure.trace().longAggregateMsg)
    }
  }

  def parseSymbols(input: String): Either[String, List[Symbol]] = {
    def symbolsEntry[$: P]: P[List[Symbol]] = P(Start ~ symbolList ~ End)

    parse(input, symbolsEntry(_)) match {
      case Parsed.Success(result, _) => Right(result)
      case failure: Parsed.Failure => Left(s"Error parsing symbols: ${failure.trace().longAggregateMsg}")
    }
  }

  def parseRule(input: String): Either[String, Rule] = {
    def ruleEntry[$: P]: P[Rule] = P(Start ~ rule ~ End)

    parse(input, ruleEntry(_)) match {
      case Parsed.Success(result, _) => Right(result)
      case failure: Parsed.Failure => Left(s"Error parsing rule: ${failure.trace().longAggregateMsg}")
    }
  }
}