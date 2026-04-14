package com.fractala.core.traits

import com.fractala.core.models.Symbol

/** Defines the rewriting rules (productions) for an L-System.
  */
trait Grammar {

  /** Applies a production rule to a given symbol.
    *
    * @param symbol
    *   The symbol to expand.
    * @return
    *   A list of symbols resulting from the production rule.
    */
  def applyProduction(symbol: Symbol): List[Symbol]
}
