package com.fractala.core.traits

import com.fractala.core.models.{Color, Rule, Symbol}
import com.fractala.core.parser.{DslResult, ParseError}

/** Defines the contract for parsing text inputs into L-System components. It supports parsing full custom DSL documents
  * as well as extracting individual elements like rules and symbols.
  */
trait Parser {

  /** Parses a complete L-System DSL document. This includes extracting the configuration block, the starting axiom, and
    * the production rules.
    *
    * @param input
    *   The raw string containing the full DSL definition (Config, Axiom, and Rules).
    * @return
    *   An `Either` containing the parsed [[DslResult]] on success, or [[ParseError]] on failure.
    */
  def parseDsl(input: String): Either[ParseError, DslResult]

  /** Parses a sequence of characters into a list of L-System symbols.
    *
    * @param input
    *   The raw string containing L-System characters and tags (e.g., "F [ + X ] <red> F").
    * @param palette
    *   A map of available colors. Required if the input contains color change symbols.
    * @return
    *   An `Either` containing a list of parsed [[Symbol]] objects on success, or [[ParseError]] on failure.
    */
  def parseSymbols(input: String, palette: Map[String, Color] = Map.empty): Either[ParseError, List[Symbol]]

  /** Parses a single production rule definition.
    *
    * @param input
    *   The string definition of the rule (e.g., "F (0.50) -> <green> F [ + F ]").
    * @param palette
    *   A map of available colors. Required if the rule's successor contains color change symbols.
    * @return
    *   An `Either` containing the parsed [[Rule]] object on success, or [[ParseError]] on failure.
    */
  def parseRule(input: String, palette: Map[String, Color] = Map.empty): Either[ParseError, Rule]
}
