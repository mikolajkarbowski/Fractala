package com.fractala.core.models

import com.fractala.core.models.Color

/** Represents instructions for a renderer to draw basic geometric shapes.
  */
enum DrawingInstruction {

  /** Instruction to draw a straight line.
    *
    * @param xFrom
    *   The starting X coordinate.
    * @param yFrom
    *   The starting Y coordinate.
    * @param xTo
    *   The ending X coordinate.
    * @param yTo
    *   The ending Y coordinate.
    * @param lineWidth
    *   The width of the line to draw.
    * @param color
    *   The color of the line.
    */
  case DrawLine(xFrom: Double, yFrom: Double, xTo: Double, yTo: Double, lineWidth: Double, color: Color)

  /** Instruction to draw a dot at the current position.
    *
    * @param x
    *   The X coordinate of the dot.
    * @param y
    *   The Y coordinate of the dot.
    * @param radius
    *   The radius (width) of the dot.
    */
  case DrawDot(x: Double, y: Double, radius: Double)
}
