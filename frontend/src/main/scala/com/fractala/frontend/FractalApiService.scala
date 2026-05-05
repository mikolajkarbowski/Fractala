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

class FractalApiService(apiUrl: String):

  /** Wysyła kod L-Script do backendu i streamuje instrukcje rysowania przez SSE (Server-Sent Events)
    */
  def renderFractal(
      code: String,
      onInstruction: DrawingInstruction => Unit,
      onError: String => Unit,
      onComplete: () => Unit
  ): Future[Unit] =
    val promise = Promise[Unit]()

    val request = FractalRequest(code)
    val requestBody = request.asJson.noSpaces

    dom.console.log(s"[API] Wysyłanie zapytania na: $apiUrl")
    dom.console.log(s"[API] Body: $requestBody")

    val fetchOptions = js.Dynamic.literal(
      method = "POST",
      headers = js.Dynamic.literal(
        "Content-Type" -> "application/json"
      ),
      body = requestBody
    )

    dom
      .fetch(apiUrl, fetchOptions.asInstanceOf[dom.RequestInit])
      .toFuture
      .flatMap { response =>
        if (!response.ok) {
          response.text().toFuture.map { errorText =>
            val errorMsg = s"Błąd serwera (${response.status}): $errorText"
            dom.console.error(s"[API ERROR] $errorMsg")
            onError(errorMsg)
            promise.failure(new Exception(errorMsg))
          }
        } else {
          dom.console.log("[API] Połączono ze strumieniem. Oczekuję na dane...")

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
                            dom.console.error(s"[JSON PARSE ERROR] Ostatni chunk: $jsonStr", error.getMessage)
                      }
                    }
                  }

                  dom.console.log(s"[API] Strumień zakończony. Narysowano $instructionCount instrukcji.")
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
                              dom.console.error(s"[JSON PARSE ERROR] Błąd parsowania: $jsonStr", error.getMessage)
                            case null =>
                              dom.console.error(s"[JSON PARSE ERROR] Błąd parsowania")
                        }
                      }
                    }
                  }

                  // Rekurencyjne czytanie następnego chunka
                  readChunk()
                }
              }
              .recoverWith { case error =>
                val errorMsg = s"Błąd podczas czytania strumienia: ${error.getMessage}"
                dom.console.error(s"[STREAM ERROR] $errorMsg")
                onError(errorMsg)
                promise.failure(error)
                Future.failed(error)
              }

          readChunk()
        }
      }
      .recoverWith { case error =>
        val errorMsg = s"Błąd połączenia: ${error.getMessage}"
        dom.console.error(s"[FETCH ERROR] $errorMsg")
        onError(errorMsg)
        promise.failure(error)
        Future.failed(error)
      }

    promise.future
