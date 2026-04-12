package com.fractala.api

import cats.effect.{IO, IOApp, ExitCode}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import org.slf4j.LoggerFactory

import com.fractala.api.controllers.{
  FractalsCatalogController,
  FractalsRenderingController
}
import com.fractala.api.services.{
  JsonFractalsCatalogService,
  NdjsonFractalsRenderingService
}

import com.fractala.api.middlewares.RequestLoggerMiddleware

object Main extends IOApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Starting Interactive Fractal API...")

    val fractalsCatalogService = new JsonFractalsCatalogService()
    val fractalsCatalogController = new FractalsCatalogController(
      fractalsCatalogService
    )

    val fractalsRenderingService = new NdjsonFractalsRenderingService()
    val fractalsRenderingController = new FractalsRenderingController(
      fractalsRenderingService
    )

    val allEndpoints =
      fractalsCatalogController.endpoints ++ fractalsRenderingController.endpoints

    val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](
      allEndpoints,
      "Fractala API",
      "1.0.0"
    )

    val allRoutes = Http4sServerInterpreter[IO]().toRoutes(
      allEndpoints ++ swaggerEndpoints
    )

    val baseHttpApp = Router[IO]("/" -> allRoutes).orNotFound
    val loggedHttpApp = RequestLoggerMiddleware(baseHttpApp)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(loggedHttpApp)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
