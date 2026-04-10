package com.fractala.core.iterator

import com.fractala.core.grammar.StochasticGrammar
import com.fractala.core.models.Color.{Black, Blue, Red}
import com.fractala.core.models.Config
import com.fractala.core.models.DrawingInstruction.{DrawDot, DrawLine}
import com.fractala.core.models.Symbol.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RecursiveLSystemIteratorSpec extends AnyFlatSpec with Matchers {
  val defaultConfig: Config = Config(
    lineLength = 10.0,
    lineWidth = 2.0,
    turningAngle = 90.0,
    lineWidthIncrement = 1.0,
    lineWidthMultiplier = 2.0,
    turningAngleIncrement = 10.0,
    startingColor = Black
  )

  val emptyGrammar = StochasticGrammar()

  "RecursiveLSystemIterator" should "return empty iterator for empty axiom" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    iterator.iterate(Nil, emptyGrammar, 0).toList shouldBe empty
  }

  it should "handle DrawForward at level 0" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(DrawForward), emptyGrammar, 0).toList

    instructions should have size 1
    instructions.head should matchPattern {
      case DrawLine(0.0, 0.0, 0.0, 10.0, 2.0, Black) =>
    }
  }

  it should "handle MoveForward at level 0" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(MoveForward, DrawForward), emptyGrammar, 0).toList

    instructions should have size 1
    // After MoveForward, position is (0, 10), orientation is (0, 1)
    // Next DrawForward starts from (0, 10) and goes to (0, 20)
    instructions.head should matchPattern {
      case DrawLine(0.0, 10.0, 0.0, 20.0, 2.0, Black) =>
    }
  }

  it should "handle TurnLeft" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(TurnLeft, DrawForward), emptyGrammar, 0).toList

    instructions should have size 1
    // Initial orientation (0, 1). TurnLeft 90 deg -> (-1, 0)
    // Start (0, 0), end (0 + -1*10, 0 + 0*10) = (-10, 0)
    val line = instructions.head.asInstanceOf[DrawLine]
    line.xFrom shouldBe 0.0 +- 1e-9
    line.yFrom shouldBe 0.0 +- 1e-9
    line.xTo shouldBe -10.0 +- 1e-9
    line.yTo shouldBe 0.0 +- 1e-9
  }

  it should "handle TurnRight" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(TurnRight, DrawForward), emptyGrammar, 0).toList

    instructions should have size 1
    // Initial orientation (0, 1). TurnRight 90 deg -> (1, 0)
    // Start (0, 0), end (10, 0)
    val line = instructions.head.asInstanceOf[DrawLine]
    line.xFrom shouldBe 0.0 +- 1e-9
    line.yFrom shouldBe 0.0 +- 1e-9
    line.xTo shouldBe 10.0 +- 1e-9
    line.yTo shouldBe 0.0 +- 1e-9
  }

  it should "handle ReverseDirection" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(ReverseDirection, DrawForward), emptyGrammar, 0).toList

    instructions should have size 1
    // Initial orientation (0, 1). Reverse -> (0, -1)
    // Start (0, 0), end (0, -10)
    val line = instructions.head.asInstanceOf[DrawLine]
    line.xTo shouldBe 0.0 +- 1e-9
    line.yTo shouldBe -10.0 +- 1e-9
  }

  it should "handle StackPush and StackPop" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    // Push, Move, Pop, Draw
    val instructions = iterator.iterate(List(StackPush, MoveForward, StackPop, DrawForward), emptyGrammar, 0).toList

    instructions should have size 1
    // After MoveForward, pos is (0, 10). Pop restores it to (0, 0).
    // DrawForward should go from (0, 0) to (0, 10).
    instructions.head should matchPattern {
      case DrawLine(0.0, 0.0, 0.0, 10.0, 2.0, Black) =>
    }
  }

  it should "handle Dot" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(MoveForward, Dot), emptyGrammar, 0).toList

    instructions should have size 1
    instructions.head should matchPattern {
      case DrawDot(0.0, 10.0, 2.0) =>
    }
  }

  it should "handle LineWidth modifications" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val symbols = List(
      IncrementLineWidth, DrawForward, // 2+1 = 3
      DecrementLineWidth, DrawForward, // 3-1 = 2
      ScaleUpLineWidth, DrawForward,   // 2*2 = 4
      ScaleDownLineWidth, DrawForward  // 4/2 = 2
    )
    val instructions = iterator.iterate(symbols, emptyGrammar, 0).toList

    instructions should have size 4
    instructions(0).asInstanceOf[DrawLine].lineWidth shouldBe 3.0
    instructions(1).asInstanceOf[DrawLine].lineWidth shouldBe 2.0
    instructions(2).asInstanceOf[DrawLine].lineWidth shouldBe 4.0
    instructions(3).asInstanceOf[DrawLine].lineWidth shouldBe 2.0
  }

  it should "handle TurningAngle modifications" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    // turningAngle = 90. IncrementTurningAngle -> 100.
    // TurnLeft by 100.
    val symbols = List(IncrementTurningAngle, TurnLeft, DrawForward)
    val instructions = iterator.iterate(symbols, emptyGrammar, 0).toList

    instructions should have size 1
    val line = instructions.head.asInstanceOf[DrawLine]
    // angle = 100 deg. orient = (cos(100+90), sin(100+90)) in math terms if 0 is X axis.
    // Here we start with (0, 1) which is 90 deg.
    // Rotate by 100 deg CCW -> 190 deg.
    // cos(190) = -0.9848, sin(190) = -0.1736
    line.xTo shouldBe (10.0 * Math.cos(Math.toRadians(190))) +- 1e-6
    line.yTo shouldBe (10.0 * Math.sin(Math.toRadians(190))) +- 1e-6

    // Now decrement turning angle twice: 100 - 10 - 10 = 80.
    val symbols2 = List(DecrementTurningAngle, DecrementTurningAngle, TurnRight, DrawForward)
    // We are at current orientation from previous test if we continued, but iterate starts fresh.
    val iterator2 = RecursiveLSystemIterator(defaultConfig)
    val instructions2 = iterator2.iterate(symbols2, emptyGrammar, 0).toList
    val line2 = instructions2.head.asInstanceOf[DrawLine]
    // Start (0, 1) = 90 deg. TurnRight 70 deg -> 20 deg.
    // cos(20) = 0.93969, sin(20) = 0.34202
    line2.xTo shouldBe (10.0 * Math.cos(Math.toRadians(20))) +- 1e-6
    line2.yTo shouldBe (10.0 * Math.sin(Math.toRadians(20))) +- 1e-6
  }

  it should "handle ColorChange" in {
    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(ColorChange(Red), DrawForward, ColorChange(Blue), DrawForward), emptyGrammar, 0).toList

    instructions should have size 2
    instructions(0).asInstanceOf[DrawLine].color shouldBe Red
    instructions(1).asInstanceOf[DrawLine].color shouldBe Blue
  }

  it should "expand grammar level 1" in {
    val grammar = StochasticGrammar()
    // Rule: F -> F+F-F
    grammar.addProduction(DrawForward, 1.0, List(DrawForward, TurnLeft, DrawForward, TurnRight, DrawForward))

    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions = iterator.iterate(List(DrawForward), grammar, 1).toList

    // Should be F+F-F: 3 DrawLine instructions
    instructions should have size 3
    instructions(0).asInstanceOf[DrawLine].xFrom shouldBe 0.0
    instructions(0).asInstanceOf[DrawLine].yTo shouldBe 10.0

    instructions(1).asInstanceOf[DrawLine].xFrom shouldBe 0.0
    instructions(1).asInstanceOf[DrawLine].yFrom shouldBe 10.0
    // After F, TurnLeft (90), orient is (-1, 0)
    instructions(1).asInstanceOf[DrawLine].xTo shouldBe -10.0 +- 1e-9
    instructions(1).asInstanceOf[DrawLine].yTo shouldBe 10.0 +- 1e-9

    instructions(2).asInstanceOf[DrawLine].xFrom shouldBe -10.0 +- 1e-9
    instructions(2).asInstanceOf[DrawLine].yFrom shouldBe 10.0 +- 1e-9
    // After F+F, TurnRight (90), orient is (-1,0) rotate by -90 -> (0, 1)
    instructions(2).asInstanceOf[DrawLine].xTo shouldBe -10.0 +- 1e-9
    instructions(2).asInstanceOf[DrawLine].yTo shouldBe 20.0 +- 1e-9
  }

  it should "expand grammar level 2" in {
    val grammar = StochasticGrammar()
    // Rule: A -> B, B -> F
    grammar.addProduction(Variable('A'), 1.0, List(Variable('B')))
    grammar.addProduction(Variable('B'), 1.0, List(DrawForward))

    val iterator = RecursiveLSystemIterator(defaultConfig)
    val instructions0 = iterator.iterate(List(Variable('A')), grammar, 0).toList
    instructions0 shouldBe empty

    val instructions1 = iterator.iterate(List(Variable('A')), grammar, 1).toList
    instructions1 shouldBe empty // A -> B, B doesn't draw

    val instructions2 = iterator.iterate(List(Variable('A')), grammar, 2).toList
    instructions2 should have size 1 // A -> B -> F
    instructions2.head should matchPattern { case DrawLine(_, _, _, _, _, _) => }
  }
}
