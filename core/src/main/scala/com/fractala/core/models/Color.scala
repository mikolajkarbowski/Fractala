package com.fractala.core.models

/**
 * Represents a drawing color in RGB format.
 */
case class Color(r: Double, g: Double, b: Double)

object Color{
  private val namedColors: Map[String, Color] = Map(
    "red" -> Color(1.0, 0.0, 0.0),
    "green" -> Color(0.0, 1.0, 0.0),
    "blue" -> Color(0.0, 0.0, 1.0),
    "black" -> Color(0.0, 0.0, 0.0),
    "white" -> Color(1.0, 1.0, 1.0),
    "gray" -> Color(0.5, 0.5, 0.5),
    "brown" -> Color(0.54, 0.27, 0.07),
    "yellow" -> Color(1.0, 1.0, 0.0),
    "orange" -> Color(1.0, 0.65, 0.0),
    "purple" -> Color(0.5, 0.0, 0.5),
    "pink" -> Color(1.0, 0.75, 0.8)
  )

  def from(name: String) : Option[Color] = namedColors.get(name.toLowerCase)
}