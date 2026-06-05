package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Canvas, Div}
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** The editor page: a DSL code editor on the left and a rendering canvas on the right.
  *
  * @param apiService
  *   used to render code and to fetch examples for deep-linked example URLs.
  * @param navigate
  *   navigates to another route (unused for now, kept for symmetry with other views).
  */
class EditorView(apiService: FractalApiService, navigate: String => Unit):

  private val defaultCode = """Config {
  lineLength: 30.0
  turningAngle: 22.5
  maxIterations: 7
  startingColor: stalk
}

Colors {
  stalk: 0.45, 0.35, 0.15
  autumnRed: 0.85, 0.20, 0.15
  autumnGold: 0.95, 0.65, 0.10
}

Axiom: X

Rules {
  X (0.5) -> <stalk> F - [ [ X ] + X ] + F [ + <autumnRed> F X ] - X
  X (0.5) -> <stalk> F - [ [ X ] + X ] + F [ + <autumnGold> F X ] - X
  F -> F F
}"""

  private val canvas: Canvas =
    val c = document.createElement("canvas").asInstanceOf[Canvas]
    c.id = "fractalCanvas"
    c.width = 800
    c.height = 800
    c

  private val renderer = new CanvasRenderer(canvas)

  private var isRendering = false

  private val errorDiv = div(id := "errorMessage", cls := "error-msg").render

  private val inputArea = textarea(
    id := "dslInput",
    cls := "code-editor",
    spellcheck := false,
    defaultCode
  ).render

  private val statusDiv = div(cls := "status-msg").render

  private val sendButton = button(
    id := "sendBtn",
    cls := "btn-primary",
    "Generate",
    onclick := { (_: dom.Event) => handleRender() }
  ).render

  private val leftPanel = div(
    cls := "left-panel",
    h2("Fractala Editor"),
    errorDiv,
    inputArea,
    sendButton,
    statusDiv
  ).render

  private val rightPanel = div(cls := "right-panel", canvas).render

  val rootElement: Div = div(cls := "app-container", leftPanel, rightPanel).render

  /** Fetches an example by id, loads its code into the editor, and renders it automatically. */
  def loadExample(id: String): Unit =
    errorDiv.textContent = ""
    statusDiv.textContent = "Loading example..."
    apiService.fetchExample(id).foreach {
      case Right(example) =>
        inputArea.value = example.code
        statusDiv.textContent = s"Loaded example: ${example.name}"
        handleRender()
      case Left(error) =>
        statusDiv.textContent = ""
        errorDiv.textContent = error
    }

  private def handleRender(): Unit =
    if isRendering then return

    val code = inputArea.value
    errorDiv.textContent = ""
    statusDiv.textContent = ""
    isRendering = true
    sendButton.disabled = true
    sendButton.textContent = "Drawing..."

    renderer.clear()
    var instructionCount = 0

    apiService.renderFractal(
      code = code,
      onInstruction = { instruction =>
        instructionCount += 1
        renderer.drawInstruction(instruction)
        if instructionCount % 100 == 0 then statusDiv.textContent = s"Drawn $instructionCount instructions..."
      },
      onError = { error =>
        errorDiv.textContent = error
        statusDiv.textContent = ""
        resetButton()
      },
      onComplete = { () =>
        statusDiv.textContent = s"Done! Drawn $instructionCount instructions."
        resetButton()
      }
    )

  private def resetButton(): Unit =
    isRendering = false
    sendButton.disabled = false
    sendButton.textContent = "Generate"
