package com.fractala.core.traits

import com.fractala.core.models.Symbol

trait LSystemIterator {
  def iterate(axiom: List[Symbol], grammar: Grammar, level: Int): Iterator[Symbol]
}
