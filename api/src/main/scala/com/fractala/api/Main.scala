package com.fractala.api

import cats.effect.{IO, IOApp, ExitCode}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.generic.derivation.default.*

import com.fractala.api.controllers.*
import com.fractala.api.services.*
import com.fractala.api.services.contracts.*
import com.fractala.api.models.AppConfig
import com.fractala.api.middlewares.RequestLoggerMiddleware

object Main extends IOApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private def combineEndpoints() = {
    given fractalsCatalogService: FractalsCatalogService = new JsonFractalsCatalogService()
    given fractalsRenderingService: FractalsRenderingService = new NdjsonFractalsRenderingService()

    val fractalsCatalogController = new FractalsCatalogController()
    val fractalsRenderingController = new FractalsRenderingController()

    val allEndpoints =
      fractalsCatalogController.endpoints ++ fractalsRenderingController.endpoints

    val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](
      allEndpoints,
      "Fractala API",
      "1.0.0"
    )

    allEndpoints ++ swaggerEndpoints
  }

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      _ <- IO(logger.info("Starting Fractala API..."))

      config <- IO.delay(ConfigSource.default.loadOrThrow[AppConfig])
      _ = logger.info("Initializing services and controllers...")
      allEndpoints = combineEndpoints()
      allRoutes = Http4sServerInterpreter[IO]().toRoutes(allEndpoints)
      
      baseHttpApp = Router[IO]("/" -> allRoutes).orNotFound
      loggedHttpApp = RequestLoggerMiddleware(baseHttpApp)

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
        .withHttpApp(loggedHttpApp)
        .build
        .use { server =>
          val host = server.address.getHostString
          val displayHost = if (host == "0:0:0:0:0:0:0:0" || host == "0.0.0.0") "localhost" else host

          IO(logger.info(s"Server successfully started and is listening on http://$displayHost:${server.address.getPort}")) *>
          IO(logger.info(s"Swagger UI available at http://$displayHost:${server.address.getPort}/docs")) *>
          IO.never
        }

    } yield ExitCode.Success
  }
}
