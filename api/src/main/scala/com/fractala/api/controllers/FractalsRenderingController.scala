package com.fractala.api.controllers

import cats.MonadThrow
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import sttp.model.sse.ServerSentEvent
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.serverSentEventsBody
import sttp.capabilities.fs2.Fs2Streams

import com.fractala.api.services.contracts.FractalsRenderingService
import com.fractala.api.models.responses.ErrorResponse
import com.fractala.api.models.requests.RenderRequest

class FractalsRenderingController[F[_]: MonadThrow](using renderingService: FractalsRenderingService[F]) {

  private val renderEndpoint = endpoint.post
    .in("fractals" / "render")
    .in(
      jsonBody[RenderRequest].description(
        "L-System textual representation and recursion level"
      )
    )
    .out(serverSentEventsBody[F])
    .errorOut(jsonBody[ErrorResponse])
    .name("Stream Fractal Rendering Instructions")
    .description(
      "Streams drawing instructions using SSE based on provided raw Fractala code."
    )

  val renderServerLogic: ServerEndpoint[Fs2Streams[F], F] =
    renderEndpoint.serverLogic { request =>
      renderingService.streamFractalInstructions(request.code).attempt.map {
        case Left(error: IllegalArgumentException) =>
          Left(ErrorResponse.badRequest(error.getMessage))

        case Left(error) =>
          Left(ErrorResponse.internalServerError(s"An unexpected error occurred: ${error.getMessage}"))

        case Right(None) =>
          Left(ErrorResponse.unprocessableEntity("Failed to process the L-System code."))

        case Right(Some(instructionStream)) =>
          val sseStream = instructionStream.map { instruction =>
            ServerSentEvent(
              data = Some(instruction.asJson.noSpaces),
              eventType = Some("instruction")
            )
          }
          Right(sseStream)
      }
    }

  val endpoints: List[ServerEndpoint[Fs2Streams[F], F]] = List(
    renderServerLogic
  )
}
