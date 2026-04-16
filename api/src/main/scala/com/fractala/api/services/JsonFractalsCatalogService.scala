package com.fractala.api.services

import cats.Applicative
import cats.syntax.all.*
import cats.syntax.applicative.*
import cats.effect.Sync
import io.circe.parser.*
import io.circe.generic.auto.*
import scala.io.Source
import java.util.UUID

import com.fractala.api.services.contracts.FractalsCatalogService
import com.fractala.api.models.responses.{FractalResponse, PaginatedResponse}

class JsonFractalsCatalogService[F[_]: Applicative](
    private val fractalsList: List[FractalResponse]
) extends FractalsCatalogService[F] {

  override def getFractal(id: UUID): F[Option[FractalResponse]] = {
    fractalsList.find(_.id == id).pure[F]
  }

  override def getFractals(
      limit: Int,
      offset: Int
  ): F[PaginatedResponse[FractalResponse]] = {
    PaginatedResponse.fromList(fractalsList, limit, offset).pure[F]
  }
}

object JsonFractalsCatalogService {

  def make[F[_]: Sync]: F[JsonFractalsCatalogService[F]] = {
    Sync[F]
      .blocking {
        Source.fromResource("fractals.json").mkString
      }
      .flatMap { jsonString =>
        decode[List[FractalResponse]](jsonString) match {
          case Right(fractals) =>
            val sorted = fractals.sortBy(_.name)
            Sync[F].pure(new JsonFractalsCatalogService[F](sorted))
          case Left(error) =>
            Sync[F].raiseError(new RuntimeException(s"Failed to parse fractals catalog: ${error.getMessage}"))
        }
      }
  }
}
