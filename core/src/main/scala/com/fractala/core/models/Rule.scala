package com.fractala.core.models

/** Represents a single production rule in the L-System.
  *
  * @param predecessor
  *   The input symbol (left-hand side of the rule) to be replaced.
  * @param weight
  *   The relative weight (probability) of selecting this rule. Defaults to 1.0 for deterministic rules.
  * @param successor
  *   The list of symbols (right-hand side of the rule) that will replace the predecessor.
  */
case class Rule(
    predecessor: Symbol,
    weight: Double = 1.0,
    successor: List[Symbol]
)
