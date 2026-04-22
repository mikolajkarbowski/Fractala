package com.fractala.core.parser.ast

/** The base trait for all parsed fields within the `Config { ... }` block during the AST generation phase. It ensures
  * that every configuration field natively exposes its identifier name, making validation (e.g., duplicate checking)
  * completely type-safe and straightforward.
  *
  * @param fieldName
  *   The name of the configuration key (normalized to lowercase).
  */
sealed trait AstConfigField(val fieldName: String)

/** Represents a configuration field that holds a double-precision floating-point value (e.g., lineLength,
  * turningAngle).
  *
  * @param fieldName
  *   The name of the configuration key.
  * @param value
  *   The parsed numeric value.
  */
case class AstDoubleField(override val fieldName: String, value: Double) extends AstConfigField(fieldName)

/** Represents a configuration field that holds an integer value (e.g., maxIterations).
  *
  * @param fieldName
  *   The name of the configuration key.
  * @param value
  *   The parsed integer value.
  */
case class AstIntField(override val fieldName: String, value: Int) extends AstConfigField(fieldName)

/** Represents a configuration field that holds an unresolved color reference (e.g., startingColor: brown). At the AST
  * phase, the color is stored purely as a string identifier and awaits semantic validation against a defined palette.
  *
  * @param fieldName
  *   The name of the configuration key.
  * @param colorName
  *   The name of the requested color (e.g., "brown", "red").
  * @param index
  *   The absolute character index in the source string, used for precise error reporting if the color cannot be
  *   resolved in the semantic phase.
  */
case class AstColorField(override val fieldName: String, colorName: String, index: Int)
    extends AstConfigField(fieldName)
