package com.fractala.core.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Unit tests for the [[StochasticGrammar]] class.
 *
 * This test suite covers the following:
 * - Default behavior when no production rules are defined for a symbol.
 * - Deterministic behavior with a single production rule.
 * - Stochastic behavior when multiple production rules are defined for the same symbol.
 * - Determinism across different instances when the same seed is used.
 */
class StochasticGrammarSpec extends AnyFlatSpec with Matchers {

  "A StochasticGrammar" should "return the symbol itself in a list if no production rule is defined" in {
    val grammar = new StochasticGrammar()
    val symbol = Symbol.DrawForward
    grammar.applyProduction(symbol) shouldBe List(symbol)
  }

  it should "return a deterministic list of symbols when a single production rule is defined" in {
    val grammar = new StochasticGrammar()
    val lSide = Symbol.Variable('A')
    val rSide = List(Symbol.DrawForward, Symbol.TurnLeft, Symbol.Variable('B'))
    
    grammar.addProduction(lSide, 1.0f, rSide)
    
    grammar.applyProduction(lSide) shouldBe rSide
  }

  it should "probabilistically choose between multiple production rules based on their weights" in {
    // Using a fixed seed for reproducibility within this test if needed,
    // although we are checking distribution here.
    val grammar = new StochasticGrammar(12345L)
    val lSide = Symbol.Variable('A')
    
    val rSide1 = List(Symbol.DrawForward)
    val rSide2 = List(Symbol.TurnLeft)
    
    // Equal weights
    grammar.addProduction(lSide, 1.0f, rSide1)
    grammar.addProduction(lSide, 1.0f, rSide2)
    
    val iterations = 1000
    val results = (1 to iterations).map(_ => grammar.applyProduction(lSide))
    
    val count1 = results.count(_ == rSide1)
    val count2 = results.count(_ == rSide2)
    
    count1 should be > 0
    count2 should be > 0
    (count1 + count2) shouldBe iterations
    
    // With 1.0 weight each, distribution should be roughly 50/50.
    // We use a safe margin for statistical variance.
    count1 should (be >= 400 and be <= 600)
  }

  it should "produce identical sequences of symbols when initialized with the same seed" in {
    val seed = 99L
    val grammar1 = new StochasticGrammar(seed)
    val grammar2 = new StochasticGrammar(seed)
    
    val lSide = Symbol.Variable('X')
    val rules = List(
      (0.2f, List(Symbol.DrawForward)),
      (0.5f, List(Symbol.TurnLeft)),
      (0.3f, List(Symbol.TurnRight))
    )
    
    rules.foreach { case (w, r) =>
      grammar1.addProduction(lSide, w, r)
      grammar2.addProduction(lSide, w, r)
    }
    
    val sequence1 = (1 to 100).map(_ => grammar1.applyProduction(lSide))
    val sequence2 = (1 to 100).map(_ => grammar2.applyProduction(lSide))
    
    sequence1 shouldBe sequence2
  }

  it should "correctly handle weighted selection when weights are not normalized to 1.0" in {
    val grammar = new StochasticGrammar(42L)
    val lSide = Symbol.Variable('Z')
    
    val rSide1 = List(Symbol.Variable('A'))
    val rSide2 = List(Symbol.Variable('B'))
    
    // Total weight = 10.0
    grammar.addProduction(lSide, 2.0f, rSide1) // 20%
    grammar.addProduction(lSide, 8.0f, rSide2) // 80%
    
    val iterations = 1000
    val results = (1 to iterations).map(_ => grammar.applyProduction(lSide))
    
    val count1 = results.count(_ == rSide1)
    val count2 = results.count(_ == rSide2)
    
    // Distribution should be roughly 200 vs 800
    count1 should (be >= 150 and be <= 250)
    count2 should (be >= 750 and be <= 850)
  }
}
