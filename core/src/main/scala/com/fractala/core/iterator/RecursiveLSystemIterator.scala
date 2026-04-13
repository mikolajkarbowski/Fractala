package com.fractala.core.iterator

import breeze.linalg.{DenseMatrix, DenseVector}
import com.fractala.core.extensions.rotation2DFromDegAngle
import com.fractala.core.models.DrawingInstruction.{DrawDot, DrawLine}
import com.fractala.core.models.Symbol.*
import com.fractala.core.models.{Color, Config, DrawingInstruction, Symbol}
import com.fractala.core.traits.{Grammar, LSystemIterator}

private case class State(
  position: DenseVector[Double],
  orientation: DenseVector[Double],
  lineWidth: Double,
  lineLength: Double,
  turningAngle: Double,
  color: Color)

private case class RenderContext(state: State, stack: List[State] = Nil)

/**
 * A recursive implementation of an L-System iterator.
 * It expands symbols lazily and converts them into drawing instructions based on the turtle graphics state.
 *
 * @param config The L-System configuration.
 */
class RecursiveLSystemIterator(config: Config) extends LSystemIterator(config) {
  private val state = State(
    DenseVector(0.0, 0.0),
    DenseVector(0.0, 1.0),
    config.lineWidth,
    config.lineLength,
    config.turningAngle,
    config.startingColor)
  private val stack: List[State] = Nil

  override def iterate(axiom: List[Symbol], grammar: Grammar, level: Int): Iterator[DrawingInstruction] = {
    val initialContext = RenderContext(state, stack)
    val expandedGrammar = expandGrammar(axiom, grammar, level)

    val stateAndInstructions = expandedGrammar.scanLeft((initialContext, Option.empty[DrawingInstruction])) {
      case ((currentCtx, _), symbol) =>
        handleSymbol(currentCtx, config, symbol)
    }

    stateAndInstructions.flatMap {
      case (_, instruction) => instruction
    }
  }

  private def expandGrammar(axiom: List[Symbol], grammar: Grammar, level: Int): Iterator[Symbol] = {
    if level == 0 then {
      axiom.iterator
    } else
      axiom.iterator.flatMap { symbol =>
        val nextAxiom = grammar.applyProduction(symbol)
        expandGrammar(nextAxiom, grammar, level - 1)
      }
  }

  private def handleSymbol(ctx: RenderContext, config: Config, symbol: Symbol): (RenderContext, Option[DrawingInstruction]) = {
    val state = ctx.state

    symbol match {
      case DrawForward =>
        val startPos = state.position
        val endPos = startPos + state.orientation * config.lineLength
        val newState = state.copy(position = endPos)

        val instruction = DrawLine(
          startPos(0), startPos(1),
          endPos(0), endPos(1),
          newState.lineWidth, newState.color
        )

        (ctx.copy(state = newState), Some(instruction))

      case MoveForward =>
        val endPos = state.position + state.orientation * config.lineLength
        val newState = state.copy(position = endPos)

        (ctx.copy(state = newState), None)

      case TurnLeft =>
        val rotationMatrix = DenseMatrix.rotation2DFromDegAngle(state.turningAngle)
        val newOrientation = rotationMatrix * state.orientation
        val newState = state.copy(orientation = newOrientation)

        (ctx.copy(state = newState), None)

      case TurnRight =>
        val rotationMatrix = DenseMatrix.rotation2DFromDegAngle(-state.turningAngle)
        val newOrientation = rotationMatrix * state.orientation
        val newState = state.copy(orientation = newOrientation)

        (ctx.copy(state = newState), None)

      case ReverseDirection =>
        val newOrientation = state.orientation * -1.0
        val newState = state.copy(orientation = newOrientation)

        (ctx.copy(state = newState), None)

      case StackPush =>
        val newStack = state :: ctx.stack

        (ctx.copy(stack = newStack), None)

      case StackPop =>
        ctx.stack match {
          case savedState :: remainingStack =>
            (ctx.copy(state = savedState, stack = remainingStack), None)
          case Nil =>
            (ctx, None)
        }

      case IncrementLineWidth =>
        val newState = state.copy(lineWidth = state.lineWidth + config.lineWidthIncrement)
        (ctx.copy(state = newState), None)

      case DecrementLineWidth =>
        val newState = state.copy(lineWidth = state.lineWidth - config.lineWidthIncrement)
        (ctx.copy(state = newState), None)

      case Dot =>
        (ctx, Some(DrawDot(state.position(0), state.position(1), state.lineWidth)))

      case ScaleUpLineLength =>
        val newState = state.copy(lineLength = state.lineLength * config.lineLengthMultiplier)
        (ctx.copy(state = newState), None)

      case ScaleDownLineLength =>
        val newState = state.copy(lineLength = state.lineLength / config.lineLengthMultiplier)
        (ctx.copy(state = newState), None)

      case IncrementTurningAngle =>
        val newState = state.copy(turningAngle = state.turningAngle + config.turningAngleIncrement)
        (ctx.copy(state = newState), None)

      case DecrementTurningAngle =>
        val newState = state.copy(turningAngle = state.turningAngle - config.turningAngleIncrement)
        (ctx.copy(state = newState), None)

      case com.fractala.core.models.Symbol.Variable(_) =>
        (ctx, None)

      case com.fractala.core.models.Symbol.ColorChange(newColor) =>
        val newState = state.copy(color = newColor)
        (ctx.copy(state = newState), None)
    }
  }
}