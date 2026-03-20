package com.fractala.core.engine

import com.fractala.core.models.Symbol
import com.fractala.core.traits.{Grammar, LSystemIterator}

import scala.collection.mutable


case class Config(lineLength: Int,
                  lineWidth: Int,
                  turningAngle: Float,
                  lineWidthIncrement: Int,
                  lineWidthMultiplier: Int,
                  turningAngleIncrement: Int)

class RecursiveLSystemIterator(config: Config) extends LSystemIterator {
  private var position: Vector[Double] = Vector(0.0, 0.0)
  private var orientation: Vector[Double] = Vector(0.0, 1.0)
  private var lineWidth = config.lineWidth
  private var turningAngle = config.turningAngle
  private var stack: mutable.Stack[(Vector[Double], Vector[Double])] = mutable.Stack()

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
