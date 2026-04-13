package com.fractala.core.parser

import com.fractala.core.grammar.StochasticGrammar
import com.fractala.core.models.{Symbol, Color}
import com.fractala.core.traits.Parser

object LSystemParser extends Parser{
  def parseSymbols(input: String) : List[Symbol] = {
    val colorPattern = """^<([a-zA-Z0-9]+)>(.*)""".r

    @scala.annotation.tailrec
    def loop(remaining: String, acc: List[Symbol]): List[Symbol] = {
      if (remaining.isEmpty) acc.reverse
      else remaining match {
        case colorPattern(colorName, rest) =>
          val colorSymbol = Color.fromString(colorName) match {
            case Some(c) => Symbol.ColorChange(c)
            case None => throw new IllegalArgumentException(s"Unknown color: $colorName")
          }
          loop(rest, colorSymbol :: acc)

        case _ =>
          val char = remaining.head
          val rest = remaining.tail

          if (char == ' ') {
            loop(rest, acc)
          } else {
            Symbol.fromChar(char) match {
              case Some(sym) => loop(rest, sym :: acc)
              case None => throw new IllegalArgumentException(s"Unknown grammar symbol: $char")
            }
          }
      }
    }

    loop(input, Nil)
  }

  def parseAndAddRule(ruleDef: String, grammar: StochasticGrammar): Unit = {
    val rulePattern = """^(\S)\s*(?:\((\d+(?:\.\d+)?)\))?\s*(?:->|→)\s*(.+)$""".r

    ruleDef.trim match {
      case rulePattern(leftSideStr, weightStr, rightSideStr) =>
        val lSide = parseSymbols(leftSideStr).headOption.getOrElse(
          throw new IllegalArgumentException(s"error parsing the left side of the rule: $ruleDef")
        )

        val weight = Option(weightStr).map(_.toFloat).getOrElse(1.0f)

        val rSide = parseSymbols(rightSideStr)

        grammar.addProduction(lSide, weight, rSide)

      case _ =>
        throw new IllegalArgumentException(s"Incorrect rule format: $ruleDef. Expected 'A (p) -> B'")
    }
  }
}
