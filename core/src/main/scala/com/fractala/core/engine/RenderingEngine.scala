package com.fractala.core.engine

import com.fractala.core.models.StochasticGrammar
import com.fractala.core.models.Symbol

class RenderingEngine {
  def render(axiom: List[Symbol], grammar: StochasticGrammar, level: Int): LazyList[Symbol] = {
    if level == 0 then {
      LazyList.from(axiom.filter(_ != Symbol.Variable))
    }
    else
      LazyList.from(axiom).flatMap { symbol =>
        val nextAxiom = grammar.applyProduction(symbol)
        render(nextAxiom, grammar, level - 1)
      }
  }
}
