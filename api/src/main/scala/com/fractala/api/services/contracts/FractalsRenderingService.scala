package com.fractala.api.services.contracts

import cats.effect.IO
import fs2.Stream

import com.fractala.core.models.DrawingInstruction

trait FractalsRenderingService {
  def streamFractalInstructions(
      code: String
  ): IO[Option[Stream[IO, DrawingInstruction]]]
}
