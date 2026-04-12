package com.fractala.api.middlewares

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.{HttpApp, Request}
import org.slf4j.LoggerFactory

object RequestLoggerMiddleware {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val ignoredPrefixes = List("/docs")

  def apply(app: HttpApp[IO]): HttpApp[IO] = Kleisli { (req: Request[IO]) =>
    val path = req.uri.path.renderString
    val shouldIgnore = ignoredPrefixes.exists(path.startsWith)

    if (shouldIgnore) {
      app.run(req)
    } else {
      for {
        start <- IO.realTime
        _ <- IO(logger.info(s"-> ${req.method.name} ${req.uri}"))

        res <- app.run(req).onError { case error =>
          for {
            end <- IO.realTime
            time = (end - start).toMillis
            _ <- IO(
              logger.error(
                s"!! ${req.method.name} ${req.uri} - FAILED in ${time}ms",
                error
              )
            )
          } yield ()
        }

        end <- IO.realTime
        time = (end - start).toMillis
        _ <- IO(
          logger.info(
            s"<- ${req.method.name} ${req.uri} - ${res.status.code} in ${time}ms"
          )
        )
      } yield res
    }
  }
}
