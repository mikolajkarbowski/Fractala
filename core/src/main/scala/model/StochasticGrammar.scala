package model

import scala.collection.immutable.HashMap
import scala.util.Random

class StochasticGrammar(private val seed: Long = System.currentTimeMillis()) {
  private val random = new Random(seed)

  private var productionsMap = new HashMap[Symbol, List[WeightedSymbolsList]]

  def addProduction(lSide: Symbol, weight: Float, rSide: List[Symbol]): Unit = {
    productionsMap = productionsMap.updatedWith(lSide) {
      case Some(list) => Some(WeightedSymbolsList(weight, rSide) :: list)
      case None => Some(List(WeightedSymbolsList(weight, rSide)))
    }
  }

  def applyProduction(symbol: Symbol): List[Symbol] = {
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

case class WeightedSymbolsList(weight: Float, symbols: List[Symbol])
