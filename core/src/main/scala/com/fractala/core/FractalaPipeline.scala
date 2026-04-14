package com.fractala.core

import com.fractala.core.grammar.StochasticGrammar
import com.fractala.core.iterator.RecursiveLSystemIterator
import com.fractala.core.models.DrawingInstruction
import com.fractala.core.parser.DslParser

/**
 * The main entry point for the Fractala library.
 * * This object orchestrates the entire fractal generation pipeline by combining
 * the parsing, grammar construction, and iteration stages. It abstracts away
 * the internal domain complexity, providing a clean API for client applications.
 */
object FractalaPipeline {

  /**
   * Processes the provided L-System DSL code and generates a lazy stream of drawing instructions.
   *
   * The pipeline performs the following steps:
   * 1. Parses the raw string into a valid L-System model (`Config`, `Axiom`, `Rules`).
   * 2. Constructs a [[StochasticGrammar]] and registers the parsed production rules.
   * 3. Initializes a [[RecursiveLSystemIterator]] to evaluate the axiom over the specified generations.
   *
   * @param dslCode    The raw string containing the L-System definition in the custom DSL.
   * @param iterations The number of generations (recursion depth) to evaluate the grammar.
   * @param seed       The random seed used for resolving stochastic (probabilistic) rules.
   *                   Providing the same seed guarantees reproducible fractal generation.
   * @return An `Either` yielding a descriptive error string on the `Left` if the DSL contains syntax errors,
   *         or a lazy `Iterator` of [[DrawingInstruction]]s on the `Right` upon absolute success.
   */
  def generate(dslCode: String, iterations: Int, seed: Long): Either[String, Iterator[DrawingInstruction]] = {

    DslParser.parseDSL(dslCode) match {
      case Left(errorMsg) =>
        Left(s"L-System syntactic error:\n$errorMsg")

      case Right(dslResult) =>
        val grammar = new StochasticGrammar(seed)

        dslResult.rules.foreach { rule =>
          grammar.addProduction(rule.predecessor, rule.weight.toFloat, rule.successor)
        }

        val iterator = new RecursiveLSystemIterator(dslResult.config)
        val instructions = iterator.iterate(dslResult.axiom, grammar, iterations)

        Right(instructions)
    }
  }
}
