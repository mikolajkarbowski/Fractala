package com.fractala.core.parser

import com.fractala.core.models.{Color, Config, Rule, Symbol}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DslParserSpec extends AnyFlatSpec with Matchers {

  "DslParser.parseSymbols" should "parse a sequence of symbols including colors" in {
    val input = "F [ + X ] <red> F"
    val expected = List(
      Symbol.DrawForward,
      Symbol.StackPush,
      Symbol.TurnLeft,
      Symbol.Variable('X'),
      Symbol.StackPop,
      Symbol.ColorChange(Color(1.0, 0.0, 0.0)),
      Symbol.DrawForward
    )

    DslParser.parseSymbols(input) shouldBe Right(expected)
  }

  it should "return Left with an error message on invalid symbols" in {
    val result = DslParser.parseSymbols("F [ ? X ]")

    result.isLeft shouldBe true
  }

  "DslParser.parseRule" should "parse a deterministic rule without weights" in {
    val input = "X -> F [ + X ]"
    val expected = Rule(
      predecessor = Symbol.Variable('X'),
      weight = 1.0,
      successor = List(Symbol.DrawForward, Symbol.StackPush, Symbol.TurnLeft, Symbol.Variable('X'), Symbol.StackPop)
    )

    DslParser.parseRule(input) shouldBe Right(expected)
  }

  it should "parse a stochastic rule with weights and unicode arrows" in {
    val input = "F (0.33) → <green> F"
    val expected = Rule(
      predecessor = Symbol.DrawForward,
      weight = 0.33,
      successor = List(Symbol.ColorChange(Color(0.0, 1.0, 0.0)), Symbol.DrawForward)
    )

    DslParser.parseRule(input) shouldBe Right(expected)
  }

  "DslParser.parseDSL" should "parse a complete L-System definition ignoring comments and layout" in {
    val input = """
      // My Beautiful Fern Configuration
      Config {
        turningAngle: 25.5
        startingColor: brown
        LINELENGTH: 15.0
        maxIterations: 10
      }

      Axiom: X

      Rules {
        X -> F [ + X ]
        F (0.5) -> F F
      }
    """

    val result = DslParser.parseDsl(input)
    result.isRight shouldBe true

    val dsl = result.getOrElse(fail("Parsing the full DSL failed unexpectedly"))

    val default = Config();

    dsl.config.turningAngle shouldBe 25.5
    dsl.config.startingColor shouldBe Color.from("brown").get
    dsl.config.lineLength shouldBe 15.0
    dsl.config.lineWidth shouldBe default.lineWidth
    dsl.config.maxIterations shouldBe 10

    dsl.axiom shouldBe List(Symbol.Variable('X'))

    dsl.rules should have size 2
    dsl.rules.head.predecessor shouldBe Symbol.Variable('X')
    dsl.rules(1).weight shouldBe 0.5
  }

  it should "inject the default Config if the Config block is completely missing" in {
    val input = """
      Axiom: F
      Rules {
        F -> F + F
      }
    """

    val result = DslParser.parseDsl(input)
    result.isRight shouldBe true

    val dsl = result.getOrElse(fail("Parsing failed"))

    dsl.config shouldBe Config()
  }

  it should "fail if a config field is assigned more than once" in {
    val input =
      """Config {
            lineLength: 10.0
            lineLength: 20.0
          }
          Axiom: F
          Rules { F -> F }"""

    val result = DslParser.parseDsl(input)

    result match {
      case Left(error) =>
        error.line shouldBe 4
      case Right(_) =>
        fail("Parser should have failed due to duplicate config fields")
    }
  }
}
