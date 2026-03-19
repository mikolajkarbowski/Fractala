package com.fractala.core.traits

import com.fractala.core.models.Symbol

trait Grammar {
  def applyProduction(symbol: Symbol): List[Symbol]
}
