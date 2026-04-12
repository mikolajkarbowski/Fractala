package com.fractala.core.traits

import com.fractala.core.models.{Config, DrawingInstruction, Symbol}

/**
 * Iterates through the generations of an L-System to produce drawing instructions.
 *
 * @param config The configuration for the L-System, including angles and steps.
 */
trait LSystemIterator(config: Config) {
  /**
   * Generates drawing instructions by iteratively applying grammar rules.
   *
   * @param axiom The starting list of symbols.
   * @param grammar The set of production rules to apply.
   * @param level The recursion depth (number of iterations).
   * @return An iterator of drawing instructions to be rendered.
   */
  def iterate(axiom: List[Symbol], grammar: Grammar, level: Int): Iterator[DrawingInstruction]
}
