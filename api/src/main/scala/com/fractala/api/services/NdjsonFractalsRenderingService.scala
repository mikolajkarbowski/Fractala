package com.fractala.api.services

import cats.effect.IO
import fs2.Stream

import com.fractala.core.models.DrawingInstruction
import com.fractala.core.traits.LSystemIterator
import com.fractala.core.models.{Config, Color, Symbol}
import com.fractala.core.grammar.StochasticGrammar
import com.fractala.core.iterator.RecursiveLSystemIterator
import com.fractala.api.services.contracts.FractalsRenderingService

class NdjsonFractalsRenderingService extends FractalsRenderingService {
  override def streamFractalInstructions(
      code: String
  ): IO[Option[Stream[IO, DrawingInstruction]]] = {

    // for demonstration purposes,
    // we will parse 'code' into config, grammar and level here...
    val config: Config = Config(
      lineLength = 10.0,
      lineWidth = 2.0,
      turningAngle = 90.0,
      lineWidthIncrement = 1.0,
      lineWidthMultiplier = 2.0,
      turningAngleIncrement = 10.0,
      startingColor = Color(0, 0, 0)
    )

    val emptyGrammar = StochasticGrammar()

    val recursiveIterator = RecursiveLSystemIterator(config)
    val iterator =
      recursiveIterator.iterate(
        List(Symbol.MoveForward, Symbol.DrawForward),
        emptyGrammar,
        0
      )

    val fs2Stream: Stream[IO, DrawingInstruction] =
      Stream
        .fromIterator[IO](iterator, chunkSize = 8)
        .evalTap(_ => IO.cede)

    IO.pure(Some(fs2Stream))
  }
}
