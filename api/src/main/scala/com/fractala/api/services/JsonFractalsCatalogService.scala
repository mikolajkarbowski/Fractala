package com.fractala.api.services

import cats.effect.IO
import io.circe.parser._
import io.circe.generic.auto._
import scala.io.Source
import java.util.UUID

import com.fractala.api.services.contracts.FractalsCatalogService
import com.fractala.api.models.responses.{FractalResponse, PaginatedResponse, ErrorResponse}

class JsonFractalsCatalogService extends FractalsCatalogService {

  private val fractalsList: List[FractalResponse] = {
    val jsonString = Source.fromResource("fractals.json").mkString
    decode[List[FractalResponse]](jsonString) match {
      case Right(fractals) => fractals.sortBy(_.name)
      case Left(error) =>
        throw new RuntimeException(
          s"Failed to parse fractals catalog: ${error.getMessage}"
        )
    }
  }

  override def getFractal(id: UUID): IO[Option[FractalResponse]] = IO.pure {
    fractalsList.find(_.id == id)
  }

  override def getFractals(
      limit: Int,
      offset: Int
  ): IO[PaginatedResponse[FractalResponse]] = IO.pure {
    PaginatedResponse.fromList(fractalsList, limit, offset)
  }
}
