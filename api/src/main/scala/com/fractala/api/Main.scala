package com.fractala.api

import cats.effect.{IO, IOApp, ExitCode}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import scala.concurrent.duration.*
import org.http4s.server.middleware.Throttle
import org.http4s.{Response, Status, MediaType}
import org.http4s.headers.`Content-Type`
import org.http4s.circe.CirceEntityEncoder.*

import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import io.circe.generic.auto.*

import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.generic.derivation.default.*
import org.http4s.server.middleware.CORS

import com.fractala.api.controllers.*
import com.fractala.api.services.*
import com.fractala.api.services.contracts.*
import com.fractala.api.models.AppConfig
import com.fractala.api.middlewares.RequestLoggerMiddleware
import com.fractala.api.models.responses.ErrorResponse

object Main extends IOApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      _ <- IO(logger.info("Starting Fractala API..."))

      config <- IO.delay(ConfigSource.default.loadOrThrow[AppConfig])
      _ = logger.info("Initializing services and controllers...")

      catalogService <- JsonFractalsCatalogService.make[IO]
      renderingService = new NdjsonFractalsRenderingService[IO]()

      allEndpoints = {
        given FractalsCatalogService[IO] = catalogService
        given FractalsRenderingService[IO] = renderingService

        val fractalsCatalogController = new FractalsCatalogController[IO]()
        val fractalsRenderingController = new FractalsRenderingController[IO]()

        val baseEndpoints =
          fractalsCatalogController.endpoints ++ fractalsRenderingController.endpoints

        val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](
          baseEndpoints,
          "Fractala API",
          "1.0.0"
        )

        baseEndpoints ++ swaggerEndpoints
      }

      serverOptions = Http4sServerOptions
        .customiseInterceptors[IO]
        .exceptionHandler(ExceptionHandler.pure[IO] { ctx =>
          Some(
            ValuedEndpointOutput(
              jsonBody[ErrorResponse].and(sttp.tapir.statusCode(StatusCode.InternalServerError)),
              ErrorResponse.internalServerError("An unexpected error occurred. Please try again later.")
            )
          )
        })
        .decodeFailureHandler(
          DefaultDecodeFailureHandler(
            respond = ctx => DefaultDecodeFailureHandler.respond(ctx),
            failureMessage = ctx => DefaultDecodeFailureHandler.FailureMessages.failureMessage(ctx),
            response = (statusCode, _, msg) =>
              ValuedEndpointOutput(
                jsonBody[ErrorResponse].and(sttp.tapir.statusCode(statusCode)),
                ErrorResponse(
                  title = "Validation Error",
                  status = statusCode.code,
                  detail = msg
                )
              )
          )
        )
        .options

      allRoutes = Http4sServerInterpreter[IO](serverOptions).toRoutes(allEndpoints)

      baseHttpApp = Router[IO]("/" -> allRoutes).orNotFound

      loggedHttpApp = RequestLoggerMiddleware(baseHttpApp)

      corsHttpApp = CORS.policy.withAllowOriginAll
        .withAllowCredentials(false)
        .withAllowMethodsAll
        .withAllowHeadersAll
        .apply(loggedHttpApp)

      bucket <- Throttle.TokenBucket.local[IO](capacity = 5, refillEvery = 1.second)
      rateLimitResponse = Response[IO](Status.TooManyRequests)
        .withEntity(ErrorResponse.tooManyRequests("You've exceeded the rate limit. Please slow down."))

      throttledApp = Throttle(bucket, _ => rateLimitResponse)(corsHttpApp)

      host <- IO.fromOption(Host.fromString(config.server.host))(
        new IllegalArgumentException(s"Invalid host string: ${config.server.host}")
      )
      port <- IO.fromOption(Port.fromInt(config.server.port))(
        new IllegalArgumentException(s"Invalid port number: ${config.server.port}")
      )

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(throttledApp)
        .build
        .use { server =>
          val host = server.address.getHostString
          val displayHost = if (host == "0:0:0:0:0:0:0:0" || host == "0.0.0.0") "localhost" else host

          IO(
            logger
              .info(s"Server successfully started and is listening on http://$displayHost:${server.address.getPort}")
          ) *>
            IO(logger.info(s"Swagger UI available at http://$displayHost:${server.address.getPort}/docs")) *>
            IO.never
        }

    } yield ExitCode.Success
  }
}
