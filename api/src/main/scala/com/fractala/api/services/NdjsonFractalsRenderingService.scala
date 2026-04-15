package com.fractala.api.services

import cats.effect.IO
import fs2.Stream

import com.fractala.core.models.DrawingInstruction
import com.fractala.core.traits.LSystemIterator
import com.fractala.core.models.{Config, Color, Symbol}
import com.fractala.core.grammar.StochasticGrammar
import com.fractala.core.iterator.RecursiveLSystemIterator
import com.fractala.core.FractalaPipeline
import com.fractala.api.services.contracts.FractalsRenderingService

class NdjsonFractalsRenderingService extends FractalsRenderingService {
  override def streamFractalInstructions(
      code: String
  ): IO[Option[Stream[IO, DrawingInstruction]]] = {
    val seed = System.currentTimeMillis()

    IO.blocking(FractalaPipeline.generate(code, seed)).flatMap {
      case Right(iterator) =>
        val fs2Stream: Stream[IO, DrawingInstruction] =
          Stream
            .fromBlockingIterator[IO](iterator, chunkSize = 8)
            .evalTap(_ => IO.cede)

        IO.pure(Some(fs2Stream))

      case Left(errorMessage) =>
        IO.raiseError(new IllegalArgumentException(errorMessage))
    }
  }
}
