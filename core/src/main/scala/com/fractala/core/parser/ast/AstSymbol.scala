package com.fractala.core.parser.ast

import com.fractala.core.models.Symbol

/** The base trait for all parsed symbols during the AST generation phase. At this stage, symbols may either be fully
  * resolved standard L-System commands or unresolved requests (like color changes) that require semantic validation
  * against a context.
  */
sealed trait AstSymbol

/** Represents a standard, fully resolved L-System command (e.g., F, +, [, X). This type of symbol is immediately safe
  * to use and maps directly to the domain [[Symbol]] without requiring any further semantic analysis.
  *
  * @param symbol
  *   The concrete domain [[Symbol]] parsed from the character.
  */
case class AstStandard(symbol: Symbol) extends AstSymbol

/** Represents an unresolved color change request parsed from the DSL (e.g., `<brown>`). During the AST phase, the
  * requested color is stored purely as a string identifier and awaits semantic validation against a provided color
  * palette.
  *
  * @param colorName
  *   The name of the requested color (normalized to lowercase).
  * @param index
  *   The absolute character index in the source string. Used to provide exact line and column numbers in the error
  *   report if the color name is not defined in the palette.
  */
case class AstColorReq(colorName: String, index: Int) extends AstSymbol
