package com.fractala.core.parser.ast

import com.fractala.core.models.Symbol

/** Represents a parsed production rule within the `Rules { ... }` block during the AST generation phase. It maps a
  * single predecessor symbol to a sequence of successor symbols. The successor sequence may contain unresolved elements
  * (like temporary color references) that require later semantic validation.
  *
  * @param predecessor
  *   The target symbol to be replaced during generation. This is guaranteed by the parser to be a standard, resolvable
  *   symbol.
  * @param weight
  *   The probability weight for stochastic grammars. Defaults to 1.0 (deterministic) if no weight is explicitly
  *   provided in the DSL.
  * @param successor
  *   The sequence of raw AST symbols that will replace the predecessor.
  */
case class AstRule(predecessor: Symbol, weight: Double = 1.0, successor: List[AstSymbol])
