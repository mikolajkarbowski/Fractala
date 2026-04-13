package com.fractala.core.models

/**
 * Represents the symbols used in the L-System (Lindenmayer system).
 * These symbols include commands for drawing, movement, turtle transformations, and stack operations.
 */
enum Symbol:
  /** Draw a line forward. */
  case DrawForward
  /** Move forward without drawing a line. */
  case MoveForward
  /** Turn left by a predefined angle. */
  case TurnLeft
  /** Turn right by a predefined angle. */
  case TurnRight
  /** Reverse the current direction (turn 180 degrees). */
  case ReverseDirection
  /** Push the current turtle state (position and orientation) onto the stack. */
  case StackPush
  /** Pop the state from the stack and restore the turtle's position and orientation. */
  case StackPop
  /** Increase the line width by a fixed constant. */
  case IncrementLineWidth
  /** Decrease the line width by a fixed constant. */
  case DecrementLineWidth
  /** Draw a dot at the current turtle's position. */
  case Dot
  /** Scale up the line width by a multiplication factor. */
  case ScaleUpLineLength
  /** Scale down the line width by a division factor. */
  case ScaleDownLineLength
  /** Increase the turning angle by a constant. */
  case IncrementTurningAngle
  /** Decrease the turning angle by a constant. */
  case DecrementTurningAngle
  /** A generic variable used in rewriting rules, with a given character name. */
  case Variable(name: Char)
  /** Change the drawing color. */
  case ColorChange(color: Color)

object Symbol {
  private val symbolMap: Map[Char, Symbol] = Map(
    'F' -> Symbol.DrawForward,
    'f' -> Symbol.MoveForward,
    '+' -> Symbol.TurnLeft,
    '-' -> Symbol.TurnRight,
    '|' -> Symbol.ReverseDirection,
    '[' -> Symbol.StackPush,
    ']' -> Symbol.StackPop,
    '#' -> Symbol.IncrementLineWidth,
    '!' -> Symbol.DecrementLineWidth,
    '@' -> Symbol.Dot,
    '>' -> Symbol.ScaleUpLineLength,
    '<' -> Symbol.ScaleDownLineLength,
    '(' -> Symbol.DecrementTurningAngle,
    ')' -> Symbol.IncrementTurningAngle,
  )

  def fromChar(char: Char) : Option[Symbol] = {
    symbolMap.get(char).orElse {
      if (char.isLetterOrDigit) {
        Some(Symbol.Variable(char))
      } else {
        None
      }
    }
  }
}