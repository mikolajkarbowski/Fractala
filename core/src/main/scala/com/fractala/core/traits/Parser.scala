package com.fractala.core.traits

import com.fractala.core.models.{Rule, Symbol}
import com.fractala.core.parser.{DslResult, ParseError}

/**
 * Defines the contract for parsing text inputs into L-System components.
 * It supports parsing full custom DSL documents as well as extracting individual elements like rules and symbols.
 */
trait Parser {

  /**
   * Parses a complete L-System DSL document.
   * This includes extracting the configuration block, the starting axiom, and the production rules.
   *
   * @param input The raw string containing the full DSL definition (Config, Axiom, and Rules).
   * @return An `Either` containing the parsed [[DslResult]] on success, or [[ParseError]] on failure.
   */
  def parseDsl(input: String): Either[ParseError, DslResult]

  /**
   * Parses a sequence of characters into a list of L-System symbols.
   * This is particularly useful for parsing standalone axioms or individual symbol strings outside a full DSL document.
   *
   * @param input The raw string containing L-System characters and tags (e.g., "F [ + X ] <red> F").
   * @return An `Either` containing a list of parsed [[Symbol]] objects on success, or [[ParseError]] on failure.
   */
  def parseSymbols(input: String): Either[ParseError, List[Symbol]]

  /**
   * Parses a single production rule definition.
   * Supports deterministic rules as well as stochastic rules with weights.
   *
   * @param input The string definition of the rule (e.g., "F (0.50) -> F [ + F ]").
   * @return An `Either` containing the parsed [[Rule]] object on success, or [[ParseError]] on failure.
   */
  def parseRule(input: String): Either[ParseError, Rule]
}