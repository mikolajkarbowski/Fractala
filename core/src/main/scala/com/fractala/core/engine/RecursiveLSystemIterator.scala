package com.fractala.core.engine

import com.fractala.core.models.Symbol
import com.fractala.core.traits.{Grammar, LSystemIterator}

class RecursiveLSystemIterator extends LSystemIterator {
  override def iterate(axiom: List[Symbol], grammar: Grammar, level: Int): Iterator[Symbol] = {
    if level == 0 then {
      axiom.iterator.filter(_ != Symbol.Variable)
    }
    else
      axiom.iterator.flatMap { symbol =>
        val nextAxiom = grammar.applyProduction(symbol)
        iterate(nextAxiom, grammar, level - 1)
      }
  }
}
