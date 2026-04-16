package com.fractala.api.controllers

import cats.Functor
import cats.syntax.functor.*
import sttp.tapir.*
import sttp.tapir.Validator
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import java.util.UUID
import io.circe.generic.auto.*

import com.fractala.api.models.responses.{FractalResponse, PaginatedResponse, ErrorResponse}
import com.fractala.api.services.contracts.FractalsCatalogService

class FractalsCatalogController[F[_]: Functor](using
    catalogService: FractalsCatalogService[F]
) {

  private val getFractalsEndpoint = endpoint.get
    .in("fractals")
    .in(
      query[Int]("limit")
        .description("Maximum number of items to return")
        .default(10)
        .validate(Validator.min(1))
        .validate(Validator.max(100))
    )
    .in(query[Int]("offset").description("Number of items to skip").default(0))
    .out(
      jsonBody[PaginatedResponse[FractalResponse]].description(
        "Paginated list of fractals"
      )
    )
    .errorOut(jsonBody[ErrorResponse])
    .name("Get Fractals Catalog")
    .description(
      "Retrieves a paginated and alphabetically sorted list of example L-system fractals."
    )

  private val getFractalByIdEndpoint = endpoint.get
    .in(
      "fractals" / path[UUID]("id").description(
        "Unique identifier of the fractal"
      )
    )
    .out(jsonBody[FractalResponse].description("Single fractal details"))
    .errorOut(
      statusCode(sttp.model.StatusCode.NotFound).and(jsonBody[ErrorResponse])
    )
    .name("Get Fractal by ID")
    .description("Retrieves a single fractal example by its unique UUID.")

  val getFractalsServerLogic: ServerEndpoint[Any, F] =
    getFractalsEndpoint.serverLogic { case (limit, offset) =>
      catalogService.getFractals(limit, offset).map(Right(_))
    }

  val getFractalByIdServerLogic: ServerEndpoint[Any, F] =
    getFractalByIdEndpoint.serverLogic { id =>
      catalogService.getFractal(id).map {
        case Some(fractal) => Right(fractal)
        case None          => Left(ErrorResponse.notFound(s"Fractal with id $id not found"))
      }
    }

  val endpoints: List[ServerEndpoint[Any, F]] = List(
    getFractalsServerLogic,
    getFractalByIdServerLogic
  )
}
