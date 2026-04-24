package com.fractala.core.parser.ast

import com.fractala.core.models.Color

/** The base trait for all top-level blocks parsed during the first phase (AST generation) of the DSL. These blocks
  * represent the raw, unresolved syntactic structure of the L-System document before semantic analysis and context
  * injection (e.g., color resolution) take place.
  */
sealed trait AstBlock

/** Represents the parsed `Config { ... }` block. Contains raw configuration field assignments before they are folded
  * into the final [[Config]] domain object.
  *
  * @param fields
  *   A list of parsed configuration fields (e.g., numerical values or unresolved color requests).
  */
case class AstConfigBlock(fields: List[AstConfigField]) extends AstBlock

/** Represents the parsed `Colors { ... }` block. Defines a custom color palette that maps string identifiers to
  * concrete RGB [[Color]] objects.
  *
  * @param palette
  *   A map of color names (normalized to lowercase) to their corresponding Color instances.
  */
case class AstColorsBlock(palette: Map[String, Color]) extends AstBlock

/** Represents the parsed `Axiom: ...` block. Holds the initial starting state (seed) of the L-System as a sequence of
  * raw, unresolved symbols.
  *
  * @param axiom
  *   The list of AST symbols representing the initial state.
  */
case class AstAxiomBlock(axiom: List[AstSymbol]) extends AstBlock

/** Represents the parsed `Rules { ... }` block. Contains the raw production rules that dictate how symbols evolve over
  * successive generations.
  *
  * @param rules
  *   A list of parsed AST rules, where successors may contain unresolved color references.
  */
case class AstRulesBlock(rules: List[AstRule]) extends AstBlock
