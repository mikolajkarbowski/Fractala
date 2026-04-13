package com.fractala.api.controllers

import cats.effect.IO
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

class FractalsRenderingController(using renderingService: FractalsRenderingService) {

  private val renderEndpoint = endpoint.post
    .in("fractals" / "render")
    .in(
      jsonBody[RenderRequest].description(
        "L-System textual representation and recursion level"
      )
    )
    .out(serverSentEventsBody[IO])
    .errorOut(jsonBody[ErrorResponse])
    .name("Stream Fractal Rendering Instructions")
    .description(
      "Streams drawing instructions using SSE based on provided raw Fractala code."
    )

  val renderServerLogic: ServerEndpoint[Fs2Streams[IO], IO] =
    renderEndpoint.serverLogic { request =>
      renderingService.streamFractalInstructions(request.code).map {
        case None =>
          Left(ErrorResponse("Failed to parse or process the L-System code."))

        case Some(instructionStream) =>
          val sseStream = instructionStream.map { instruction =>
            ServerSentEvent(
              data = Some(instruction.asJson.noSpaces),
              eventType = Some("instruction")
            )
          }
          Right(sseStream)
      }
    }

  val endpoints: List[ServerEndpoint[Fs2Streams[IO], IO]] = List(
    renderServerLogic
  )
}
