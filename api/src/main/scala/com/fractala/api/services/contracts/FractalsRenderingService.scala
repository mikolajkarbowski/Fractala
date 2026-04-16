package com.fractala.api.services.contracts

import cats.effect.IO
import fs2.Stream

import com.fractala.core.models.DrawingInstruction

trait FractalsRenderingService[F[_]] {
  def streamFractalInstructions(
      code: String
  ): F[Option[Stream[F, DrawingInstruction]]]
}
