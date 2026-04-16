package com.fractala.api.services.contracts

import cats.effect.IO
import java.util.UUID

import com.fractala.api.models.responses.{FractalResponse, PaginatedResponse}

trait FractalsCatalogService[F[_]] {
  def getFractal(id: UUID): F[Option[FractalResponse]]

  def getFractals(
      limit: Int,
      offset: Int
  ): F[PaginatedResponse[FractalResponse]]
}
