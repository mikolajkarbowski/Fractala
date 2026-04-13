package com.fractala.core.parser

import com.fractala.core.models.{Color, Symbol}
import com.fractala.core.grammar.StochasticGrammar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LSystemParserSpec extends AnyFlatSpec with Matchers {

  "LSystemParser" should "parse basic axioms correctly" in {
    val input = "Ff+-|[]"
    val expected = List(
      Symbol.DrawForward, Symbol.MoveForward, Symbol.TurnLeft, Symbol.TurnRight,
      Symbol.ReverseDirection, Symbol.StackPush, Symbol.StackPop
    )
    LSystemParser.parseSymbols(input) shouldBe expected
  }

  it should "parse variables correctly" in {
    val input = "X1Y"
    val expected = List(Symbol.Variable('X'), Symbol.Variable('1'), Symbol.Variable('Y'))
    LSystemParser.parseSymbols(input) shouldBe expected
  }

  it should "ignore whitespace characters" in {
    val input = "F [ + X ]"
    val expected = List(Symbol.DrawForward, Symbol.StackPush, Symbol.TurnLeft, Symbol.Variable('X'), Symbol.StackPop)
    LSystemParser.parseSymbols(input) shouldBe expected
  }

  it should "resolve the conflict between color change and line scaling (<)" in {
    val input = "<red><X"
    val expected = List(
      Symbol.ColorChange(Color(1.0, 0.0, 0.0)),
      Symbol.ScaleDownLineLength,
      Symbol.Variable('X')
    )
    LSystemParser.parseSymbols(input) shouldBe expected
  }

  it should "throw an exception for unknown characters" in {
    val exception = intercept[IllegalArgumentException] {
      LSystemParser.parseSymbols("F?X") // '?' is not in our grammar
    }
    exception.getMessage should include("Unknown grammar symbol")
  }

  it should "throw an exception for unknown colors in tags" in {
    val exception = intercept[IllegalArgumentException] {
      LSystemParser.parseSymbols("F<magic>X")
    }
    exception.getMessage should include("Unknown color")
  }

  "parseAndAddRule" should "add a deterministic rule (without specified weight)" in {
    val grammar = new StochasticGrammar()
    LSystemParser.parseAndAddRule("F -> F[+F]", grammar)

    val result = grammar.applyProduction(Symbol.DrawForward)
    result shouldBe List(Symbol.DrawForward, Symbol.StackPush, Symbol.TurnLeft, Symbol.DrawForward, Symbol.StackPop)
  }

  it should "ignore whitespace in rule definitions" in {
    val grammar = new StochasticGrammar()
    LSystemParser.parseAndAddRule("  X  ->  F  ", grammar)

    grammar.applyProduction(Symbol.Variable('X')) shouldBe List(Symbol.DrawForward)
  }

  it should "throw an exception for an invalid rule format" in {
    val grammar = new StochasticGrammar()

    intercept[IllegalArgumentException] {
      LSystemParser.parseAndAddRule("F => F[+F]", grammar) // Invalid arrow
    }

    intercept[IllegalArgumentException] {
      LSystemParser.parseAndAddRule("F (X) -> F", grammar) // Non-numeric weight
    }

    intercept[IllegalArgumentException] {
      LSystemParser.parseAndAddRule("AB -> F", grammar)
    }
  }
}