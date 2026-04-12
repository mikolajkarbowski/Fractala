package com.fractala.api.services.contracts

import cats.effect.IO
import java.util.UUID

import com.fractala.api.responses.{
  FractalResponse,
  PaginatedResponse,
  ErrorResponse
}

trait FractalsCatalogService {
  def getFractal(id: UUID): IO[Option[FractalResponse]]
  def getFractals(
      limit: Int,
      offset: Int
  ): IO[PaginatedResponse[FractalResponse]]
}
