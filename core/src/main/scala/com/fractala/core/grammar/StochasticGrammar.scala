package com.fractala.core.grammar

import com.fractala.core.models.Symbol
import com.fractala.core.traits.Grammar

import scala.collection.immutable.HashMap
import scala.util.Random

/**
 * A weighted list of symbols representing a possible outcome of a rewriting rule.
 *
 * @param weight The weight of this rewriting rule
 * @param symbols The list of symbols that should be the result of the rewriting rule
 */
private case class WeightedSymbolsList(weight: Float, symbols: List[Symbol])

/**
 * Handles the stochastic rewriting rules (productions) for the L-System.
 * A symbol can be rewritten into several lists of symbols, each with a specific weight.
 *
 * @param seed The random seed for deterministic results across different executions.
 */
class StochasticGrammar(private val seed: Long = System.currentTimeMillis()) extends Grammar {
  private val random = new Random(seed)

  private var productionsMap = new HashMap[Symbol, List[WeightedSymbolsList]]

  /**
   * Adds a production rule for a symbol with a given weight.
   *
   * @param lSide The symbol to be rewritten (the left-hand side of the rule).
   * @param weight The relative probability of choosing this rewriting rule.
   * @param rSide The list of symbols replacing the original symbol (the right-hand side of the rule).
   */
  def addProduction(lSide: Symbol, weight: Float, rSide: List[Symbol]): Unit = {
    productionsMap = productionsMap.updatedWith(lSide) {
      case Some(list) => Some(WeightedSymbolsList(weight, rSide) :: list)
      case None => Some(List(WeightedSymbolsList(weight, rSide)))
    }
  }

  /**
   * Applies a production rule to a symbol, potentially replacing it with a list of symbols.
   * If multiple rules exist for the symbol, one is chosen based on their weights.
   * If no rules are defined, the symbol is returned as a single-element list.
   *
   * @param symbol The symbol to apply the production to.
   * @return The list of symbols replacing the input symbol.
   */
  override def applyProduction(symbol: Symbol): List[Symbol] = {
    productionsMap.get(symbol) match {
      case Some(potentialRightSides) =>
        val totalWeight = potentialRightSides.map(_.weight).sum
        val roll = random.nextFloat() * totalWeight

        val chosen = potentialRightSides.foldLeft((0.0f, Option.empty[List[Symbol]])) {
          case ((acc, found), potentialRightSide) =>
            val newAcc = acc + potentialRightSide.weight

            if (found.isEmpty && roll < newAcc) (newAcc, Some(potentialRightSide.symbols))
            else (newAcc, found)
        }._2

        chosen.getOrElse(List(symbol))
      case None => List(symbol)
    }
  }
}
