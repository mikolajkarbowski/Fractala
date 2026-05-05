package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.html._
import scalatags.JsDom.all._

class FractalUI(
    renderer: CanvasRenderer,
    apiService: FractalApiService
):

  private var isRendering = false

  private val defaultCode = """// My Beautiful Fractal
Config {
  lineLength: 10.0
  lineWidth: 2.0
  turningAngle: 25.0
  startingColor: stem
  maxIterations: 5
}

Colors {
  stem: 0.54, 0.27, 0.07  // Brown
  leaf: 0.0, 1.0, 0.0     // Green
}

Axiom: X

Rules {
  // X acts as a structural placeholder generating branches
  X -> <stem> F [ + X ] [ - X ] + F
  // F draws the actual lines and grows over time
  F -> F F <leaf> [ + F ]
}"""

  // Elementy UI
  private val errorDiv = div(
    id := "errorMessage",
    cls := "error-msg"
  ).render

  private val inputArea = textarea(
    id := "dslInput",
    cls := "code-editor",
    spellcheck := false,
    defaultCode
  ).render

  private val sendButton = button(
    id := "sendBtn",
    cls := "btn-primary",
    "Wyślij do API (Generuj)",
    onclick := { (_: dom.Event) => handleRender() }
  ).render

  private val statusDiv = div(
    cls := "status-msg"
  ).render

  // Przykładowe presety
  private val presetButtons = div(
    cls := "preset-buttons",
    h3("Przykłady:"),
    button(
      cls := "btn-preset",
      "Roślina",
      onclick := { (_: dom.Event) => loadPreset("plant") }
    ),
    button(
      cls := "btn-preset",
      "Drzewo",
      onclick := { (_: dom.Event) => loadPreset("tree") }
    ),
    button(
      cls := "btn-preset",
      "Płatek śniegu",
      onclick := { (_: dom.Event) => loadPreset("snowflake") }
    ),
    button(
      cls := "btn-preset",
      "Trójkąt Sierpińskiego",
      onclick := { (_: dom.Event) => loadPreset("sierpinski") }
    )
  ).render

  // Layout panelu lewego
  val leftPanel: Div = div(
    cls := "left-panel",
    h2("Edytor L-Script (Live SSE)"),
    errorDiv,
    inputArea,
    presetButtons,
    sendButton,
    statusDiv
  ).render

  private def handleRender(): Unit =
    if (isRendering) return

    val code = inputArea.value
    errorDiv.textContent = ""
    statusDiv.textContent = ""
    isRendering = true
    sendButton.disabled = true
    sendButton.textContent = "Rysowanie..."

    dom.console.log("=== ROZPOCZĘCIE RYSOWANIA ===")

    renderer.clear()

    var instructionCount = 0

    apiService.renderFractal(
      code = code,
      onInstruction = { instruction =>
        instructionCount += 1
        renderer.drawInstruction(instruction)
        if (instructionCount % 100 == 0) {
          statusDiv.textContent = s"Narysowano $instructionCount instrukcji..."
        }
      },
      onError = { error =>
        errorDiv.textContent = error
        statusDiv.textContent = ""
        resetButton()
      },
      onComplete = { () =>
        statusDiv.textContent = s"Gotowe! Narysowano $instructionCount instrukcji."
        resetButton()
      }
    )

  private def resetButton(): Unit =
    isRendering = false
    sendButton.disabled = false
    sendButton.textContent = "Wyślij do API (Generuj)"

  private def loadPreset(presetName: String): Unit =
    val code = presetName match
      case "plant" =>
        """// My Beautiful Fractal
Config {
  lineLength: 10.0
  lineWidth: 2.0
  turningAngle: 25.0
  startingColor: stem
  maxIterations: 5
}

Colors {
  stem: 0.54, 0.27, 0.07  // Brown
  leaf: 0.0, 1.0, 0.0     // Green
}

Axiom: X

Rules {
  X -> <stem> F [ + X ] [ - X ] + F
  F -> F F <leaf> [ + F ]
}"""

      case "tree" =>
        """// Stochastic Magic Tree
Axiom: F

Rules {
  F (0.33) -> F [ + <bloom> F ] F
  F (0.33) -> F [ - <bloom> F ] F
  F (0.34) -> F <wood> F
}

Colors {
  wood: 0.6, 0.4, 0.2
  bloom: 1.0, 0.4, 0.7
}

CONFIG {
  turningAngle: 22.5
  maxIterations: 4
  lineLength: 10.0
  lineWidth: 1.5
}"""

      case "snowflake" =>
        """// Koch Snowflake
Config {
  lineLength: 10.0
  lineWidth: 1.5
  turningAngle: 60.0
  maxIterations: 4
  startingColor: ice
}

Colors {
  ice: 0.5, 0.8, 1.0
}

Axiom: F ++ F ++ F

Rules {
  F -> F - F ++ F - F
}"""

      case "sierpinski" =>
        """// Sierpinski Triangle
Config {
  lineLength: 10.0
  lineWidth: 1.0
  turningAngle: 60.0
  maxIterations: 6
  startingColor: red
}

Colors {
  red: 1.0, 0.2, 0.2
  blue: 0.2, 0.2, 1.0
}

Axiom: F - G - G

Rules {
  F -> F - G + F + G - F
  G -> G G
}"""

      case _ => defaultCode

    inputArea.value = code
    errorDiv.textContent = ""
    statusDiv.textContent = s"Załadowano preset: $presetName"
