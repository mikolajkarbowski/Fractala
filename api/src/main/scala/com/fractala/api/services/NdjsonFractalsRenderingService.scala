package com.fractala.api.services

import cats.effect.IO
import fs2.Stream
import com.fractala.core.models.DrawingInstruction
import com.fractala.core.traits.LSystemIterator
import com.fractala.api.services.contracts.FractalsRenderingService

class NdjsonFractalsRenderingService extends FractalsRenderingService {
  override def streamFractalInstructions(
      code: String
  ): IO[Option[Stream[IO, DrawingInstruction]]] = {
    // TODO: parse code into grammar and create l-system iterator
    val standardIterator: Iterator[DrawingInstruction] = Iterator.empty

    val fs2Stream: Stream[IO, DrawingInstruction] =
      Stream.fromIterator[IO](standardIterator, chunkSize = 64)

    IO.pure(Some(fs2Stream))
  }
}
