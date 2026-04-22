package com.fractala.core.parser

import com.fractala.core.models.{Color, Config, Rule, Symbol}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DslParserSpec extends AnyFlatSpec with Matchers {

  "DslParser.parseSymbols" should "parse a sequence of symbols including colors" in {
    val input = "F [ + X ] <red> F"

    val palette = Map("red" -> Color(1.0, 0, 0))

    val expected = List(
      Symbol.DrawForward,
      Symbol.StackPush,
      Symbol.TurnLeft,
      Symbol.Variable('X'),
      Symbol.StackPop,
      Symbol.ColorChange(palette("red")),
      Symbol.DrawForward
    )

    DslParser.parseSymbols(input, palette) shouldBe Right(expected)
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

    val palette = Map("green" -> Color(0, 1.0, 0))

    val expected = Rule(
      predecessor = Symbol.DrawForward,
      weight = 0.33,
      successor = List(Symbol.ColorChange(palette("green")), Symbol.DrawForward)
    )

    DslParser.parseRule(input, palette) shouldBe Right(expected)
  }

  "DslParser.parseDSL" should "parse a complete L-System definition ignoring comments and layout" in {
    val input =
      """
      // My Beautiful Fern Configuration
      Config {
        turningAngle: 25.5
        startingColor: green
        LINELENGTH: 15.0
        maxIterations: 10
      }

      Colors {
        green: 0, 1.0, 0
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
    dsl.config.startingColor shouldBe Color(0, 1.0, 0)
    dsl.config.lineLength shouldBe 15.0
    dsl.config.lineWidth shouldBe default.lineWidth
    dsl.config.maxIterations shouldBe 10

    dsl.axiom shouldBe List(Symbol.Variable('X'))

    dsl.rules should have size 2
    dsl.rules.head.predecessor shouldBe Symbol.Variable('X')
    dsl.rules(1).weight shouldBe 0.5
  }

  "DslParser" should "successfully parse the 'Beautiful Fractal' example from the README" in {
    val input =
      """
          // My Beautiful Fern
          Config {
            lineLength: 10.0
            lineWidth: 2.0
            turningAngle: 25.0
            startingColor: stem
            maxIterations: 5
          }

          Colors {
            stem: 0.54, 0.27, 0.07  // Brown
            leaf: 0.0, 1.0, 0.0     // Green
          }

          Axiom: X

          Rules {
            // X acts as a structural placeholder generating branches
            X -> <stem> F [ + X ] [ - X ] + F

            // F draws the actual lines and grows over time
            F -> F F <leaf> [ + F ]
          }
        """

    val result = DslParser.parseDsl(input)

    result.isRight shouldBe true

    val dsl = result.getOrElse(fail("Parsing failed"))

    dsl.config.lineLength shouldBe 10.0
    dsl.config.lineWidth shouldBe 2.0
    dsl.config.turningAngle shouldBe 25.0
    dsl.config.maxIterations shouldBe 5
    dsl.config.startingColor shouldBe Color(0.54, 0.27, 0.07)

    dsl.axiom shouldBe List(Symbol.Variable('X'))

    dsl.rules should have size 2
    dsl.rules.head.predecessor shouldBe Symbol.Variable('X')
    dsl.rules.head.successor.head shouldBe Symbol.ColorChange(Color(0.54, 0.27, 0.07))

    dsl.rules(1).predecessor shouldBe Symbol.DrawForward
    dsl.rules(1).successor should contain(Symbol.ColorChange(Color(0.0, 1.0, 0.0)))
  }

  it should "successfully parse the 'Stochastic Magic Tree' example highlighting order-independence" in {
    val input =
      """
          Axiom: F

          // Notice the weights in parentheses (e.g., 0.33 means 33% chance)
          Rules {
            F (0.33) -> F [ + <bloom> F ] F
            F (0.33) -> F [ - <bloom> F ] F
            F (0.34) -> F <wood> F
          }

          Colors {
            wood: 0.6, 0.4, 0.2
            bloom: 1.0, 0.4, 0.7
          }

          // Config fields are case-insensitive and optional (defaults apply if omitted)
          CONFIG {
            turningAngle: 22.5
            maxIterations: 4
          }
        """

    val result = DslParser.parseDsl(input)

    result match {
      case Left(error) => fail(s"Parsing failed with error: $error")
      case Right(dsl) =>
        dsl.config.turningAngle shouldBe 22.5
        dsl.config.maxIterations shouldBe 4

        dsl.config.lineLength shouldBe Config().lineLength

        dsl.axiom shouldBe List(Symbol.DrawForward)

        dsl.rules should have size 3
        val totalWeight = dsl.rules.map(_.weight).sum
        totalWeight shouldBe 1.0 +- 0.001

        val bloomColor = Color(1.0, 0.4, 0.7)
        dsl.rules.head.successor should contain(Symbol.ColorChange(bloomColor))
    }
  }

  it should "inject the default Config if the Config block is completely missing" in {
    val input =
      """
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
