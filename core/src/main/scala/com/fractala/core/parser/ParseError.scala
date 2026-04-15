package com.fractala.core.parser

import fastparse.Parsed.Failure

/** Represents a structured parsing error with location details.
  *
  * @param message
  *   A human-readable description of what went wrong.
  * @param line
  *   The line number where the error occurred (1-indexed).
  * @param column
  *   The column number where the error occurred (1-indexed).
  * @param index
  *   The absolute character index in the input string.
  */
case class ParseError(
    message: String = "",
    line: Int = 0,
    column: Int = 0,
    index: Int = 0
) {
  override def toString: String = s"[$line:$column] $message"
}

object ParseError {

  def from(failure: Failure): ParseError = {
    val indexRegex = """(\d+):(\d+)""".r

    val (line, col) = failure.extra.input.prettyIndex(failure.index) match {
      case indexRegex(l, c) => (l.toInt, c.toInt)
      case _                => (0, 0)
    }

    ParseError(
      line = line,
      column = col,
      message = failure.trace().longMsg,
      index = failure.index
    )
  }
}
