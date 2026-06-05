package com.fractala.frontend

import org.scalajs.dom
import io.circe.parser._
import io.circe.syntax._
import scala.scalajs.js
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.ArrayBufferView

trait TextDecodeOptions extends js.Object:
  var stream: js.UndefOr[Boolean] = js.undefined

@js.native
@JSGlobal
class TextDecoder(label: String = "utf-8") extends js.Object:
  def decode(data: ArrayBufferView, options: TextDecodeOptions = js.native): String = js.native

/** Talks to the Fractala backend. `baseUrl` is the server origin, e.g. "http://localhost:9000". */
class FractalApiService(baseUrl: String):

  private val renderUrl = s"$baseUrl/fractals/render"
  private val fractalsUrl = s"$baseUrl/fractals"

  // Matches a "[line:column]" location marker inside a parser error message.
  private val errorLocationRegex = """\[(\d+):(\d+)\]""".r

  /** Parses an API error response body into a [[RenderError]], extracting the syntax-error location if present. */
  private def parseRenderError(httpStatus: Int, body: String): RenderError =
    decode[ApiErrorBody](body) match
      case Right(parsed) =>
        val detailText = parsed.detail.filter(_.nonEmpty).getOrElse(body)
        val location = errorLocationRegex.findFirstMatchIn(detailText).map(m => (m.group(1).toInt, m.group(2).toInt))
        val statusText = parsed.status.getOrElse(httpStatus)
        val summary = parsed.title.filter(_.nonEmpty).getOrElse("Error") + s" ($statusText)"
        RenderError(summary, detailText, location.map(_._1), location.map(_._2))
      case Left(_) =>
        RenderError(s"Server error ($httpStatus)", body, None, None)

  /** Fetches the catalog of example fractals (`GET /fractals`). */
  def fetchExamples(limit: Int = 100, offset: Int = 0): Future[Either[String, List[ExampleFractal]]] =
    val url = s"$fractalsUrl?limit=$limit&offset=$offset"
    dom
      .fetch(url)
      .toFuture
      .flatMap { response =>
        if (!response.ok)
          response.text().toFuture.map { body =>
            Left(s"Failed to load examples (${response.status}): $body")
          }
        else
          response.text().toFuture.map { body =>
            decode[FractalsPage](body) match
              case Right(page) => Right(page.items)
              case Left(error) => Left(s"Failed to parse examples: ${error.getMessage}")
          }
      }
      .recover { case error =>
        Left(s"Connection error: ${error.getMessage}")
      }

  /** Fetches a single example fractal by id (`GET /fractals/{id}`). */
  def fetchExample(id: String): Future[Either[String, ExampleFractal]] =
    val url = s"$fractalsUrl/$id"
    dom
      .fetch(url)
      .toFuture
      .flatMap { response =>
        if (!response.ok)
          response.text().toFuture.map { body =>
            if (response.status == 404) Left("Example not found.")
            else Left(s"Failed to load example (${response.status}): $body")
          }
        else
          response.text().toFuture.map { body =>
            decode[ExampleFractal](body) match
              case Right(example) => Right(example)
              case Left(error)    => Left(s"Failed to parse example: ${error.getMessage}")
          }
      }
      .recover { case error =>
        Left(s"Connection error: ${error.getMessage}")
      }

  /** Sends the L-Script code to the backend and streams drawing instructions over SSE (Server-Sent Events).
    */
  def renderFractal(
      code: String,
      onInstruction: DrawingInstruction => Unit,
      onError: RenderError => Unit,
      onComplete: () => Unit
  ): Future[Unit] =
    val promise = Promise[Unit]()

    val request = FractalRequest(code)
    val requestBody = request.asJson.noSpaces

    dom.console.log(s"[API] Sending request to: $renderUrl")
    dom.console.log(s"[API] Body: $requestBody")

    val fetchOptions = js.Dynamic.literal(
      method = "POST",
      headers = js.Dynamic.literal(
        "Content-Type" -> "application/json"
      ),
      body = requestBody
    )

    dom
      .fetch(renderUrl, fetchOptions.asInstanceOf[dom.RequestInit])
      .toFuture
      .flatMap { response =>
        if (!response.ok) {
          response.text().toFuture.map { errorText =>
            val renderError = parseRenderError(response.status, errorText)
            dom.console.error(s"[API ERROR] ${renderError.summary}: ${renderError.detail}")
            onError(renderError)
            promise.failure(new Exception(renderError.summary))
          }
        } else {
          dom.console.log("[API] Connected to stream. Awaiting data...")

          val reader = response.body.getReader()
          val decoder = new TextDecoder("utf-8")

          var buffer = ""
          var instructionCount = 0

          def readChunk(): Future[Unit] =
            reader
              .read()
              .toFuture
              .flatMap { result =>
                val done = result.done
                val value = result.value

                if (done) {
                  if (buffer.trim.nonEmpty) {
                    val dataLine = buffer.split("\n").find(_.startsWith("data:"))
                    dataLine.foreach { line =>
                      val jsonStr = line.stripPrefix("data:").trim
                      if (jsonStr.nonEmpty) {
                        decode[DrawingInstruction](jsonStr) match
                          case Right(instruction) =>
                            instructionCount += 1
                            onInstruction(instruction)
                          case Left(error) =>
                            dom.console.error(s"[JSON PARSE ERROR] Last chunk: $jsonStr", error.getMessage)
                      }
                    }
                  }

                  dom.console.log(s"[API] Stream finished. Drawn $instructionCount instructions.")
                  onComplete()
                  promise.success(())
                  Future.successful(())
                } else {
                  val chunk = decoder.decode(
                    value,
                    js.Dynamic.literal(stream = true).asInstanceOf[TextDecodeOptions]
                  )
                  buffer += chunk

                  val messagesArray = buffer.split("\n\n", -1)
                  buffer = messagesArray.lastOption.getOrElse("")

                  messagesArray.dropRight(1).foreach { msg =>
                    if (msg.trim.nonEmpty) {
                      val dataLine = msg.split("\n").find(_.startsWith("data:"))
                      dataLine.foreach { line =>
                        val jsonStr = line.stripPrefix("data:").trim
                        if (jsonStr.nonEmpty) {
                          decode[DrawingInstruction](jsonStr) match
                            case Right(instruction) =>
                              instructionCount += 1
                              onInstruction(instruction)
                            case Left(error) =>
                              dom.console.error(s"[JSON PARSE ERROR] Error parsing: $jsonStr", error.getMessage)
                        }
                      }
                    }
                  }

                  // Recursively read the next chunk
                  readChunk()
                }
              }
              .recoverWith { case error =>
                val errorMsg = s"Error reading stream: ${error.getMessage}"
                dom.console.error(s"[STREAM ERROR] $errorMsg")
                onError(RenderError("Connection error", errorMsg, None, None))
                promise.failure(error)
                Future.failed(error)
              }

          readChunk()
        }
      }
      .recoverWith { case error =>
        val errorMsg = s"Connection error: ${error.getMessage}"
        dom.console.error(s"[FETCH ERROR] $errorMsg")
        onError(RenderError("Connection error", errorMsg, None, None))
        promise.failure(error)
        Future.failed(error)
      }

    promise.future
