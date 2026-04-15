package com.fractala.core.parser

import com.fractala.core.models.{Config, Rule, Symbol}

/** Represents the complete result of successfully parsing an L-System DSL document. This aggregate object holds all the
  * necessary components required to initialize and render a fractal.
  *
  * @param config
  *   The configuration parameters governing the visual rendering (e.g., line lengths, angles, colors).
  * @param axiom
  *   The initial sequence of symbols (the starting state or seed) of the L-System.
  * @param rules
  *   The list of production rules that define how the symbols evolve over successive generations.
  */
case class DslResult(config: Config, axiom: List[Symbol], rules: List[Rule])
