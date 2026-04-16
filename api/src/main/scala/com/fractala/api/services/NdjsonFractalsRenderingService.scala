package com.fractala.api.services

import cats.effect.{Async, Sync}
import cats.syntax.all.*
import fs2.Stream

import com.fractala.core.models.DrawingInstruction
import com.fractala.core.traits.LSystemIterator
import com.fractala.core.models.{Config, Color, Symbol}
import com.fractala.core.grammar.StochasticGrammar
import com.fractala.core.iterator.RecursiveLSystemIterator
import com.fractala.core.FractalaPipeline
import com.fractala.api.services.contracts.FractalsRenderingService

class NdjsonFractalsRenderingService[F[_]: Async] extends FractalsRenderingService[F] {

  override def streamFractalInstructions(
      code: String
  ): F[Option[Stream[F, DrawingInstruction]]] = {
    val seed = System.currentTimeMillis()

    Sync[F].blocking(FractalaPipeline.generate(code, seed)).flatMap {
      case Right(iterator) =>
        val fs2Stream: Stream[F, DrawingInstruction] =
          Stream
            .fromBlockingIterator[F](iterator, chunkSize = 32)
            .evalTap(_ => Async[F].cede)

        Sync[F].pure(Some(fs2Stream))

      case Left(errorMessage) =>
        Sync[F].raiseError(new IllegalArgumentException(errorMessage))
    }
  }
}
