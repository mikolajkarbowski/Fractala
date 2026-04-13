package com.fractala.core.traits

import com.fractala.core.grammar.StochasticGrammar
import com.fractala.core.models.Symbol

/**
 * Defines the contract for parsing text inputs into L-System components.
 * It handles the conversion of raw strings into symbols and the registration of production rules.
 */
trait Parser {
  /**
   * Parses a sequence of characters into a list of L-System symbols.
   *
   * @param input The raw string containing L-System characters (e.g., "F[+F]<red>F").
   * @return A list of parsed [[Symbol]] objects representing the input string.
   */
  def parseSymbols(input: String) : List[Symbol]

  /**
   * Parses a string representing a production rule and adds it to the specified grammar.
   *
   * @param ruleDef The string definition of the rule, including optional weights (e.g., "F (0.33) -> F[+F]").
   * @param grammar The [[StochasticGrammar]] instance where the parsed rule will be registered.
   */
  def parseAndAddRule(ruleDef: String, grammar: StochasticGrammar): Unit
}
