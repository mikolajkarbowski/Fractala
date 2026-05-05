package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.html._
import scalatags.JsDom.all._

class FractalUI(
    renderer: CanvasRenderer,
    apiService: FractalApiService
):

  private var isRendering = false

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

  private val autumnWeedCode = """Config {
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

  private val crystalTreeCode = """Config {
  lineLength: 80.0
  lineWidth: 2.0
  turningAngle: 25.7
  maxIterations: 6
  startingColor: bark
}

Colors {
  bark: 0.2, 0.2, 0.3
  crystalLight: 0.6, 0.9, 1.0
  crystalDark: 0.1, 0.5, 0.9
}

Axiom: F

Rules {
  F (0.4) -> <bark> F [ + F ] F [ - F ] [ F ]
  F (0.3) -> <bark> F [ + <crystalLight> F ] F [ - F ] [ F ]
  F (0.3) -> <bark> F [ + F ] F [ - <crystalDark> F ] [ F ]
}"""

  private val weepingWillowCode = """Config {
  lineLength: 20.0
  turningAngle: 35.0
  maxIterations: 8
  startingColor: wood
}

Colors {
  wood: 0.35, 0.25, 0.15
  willowGreen: 0.30, 0.70, 0.30
  paleYellow: 0.85, 0.85, 0.40
}

Axiom: X

Rules {
  X (0.4) -> <wood> F [ + X ] [ - X ] F [ - <willowGreen> X ]
  X (0.4) -> <wood> F [ - X ] [ + X ] F [ + <paleYellow> X ]
  X (0.2) -> <wood> F [ + X ] [ - X ]
  F -> F F
}"""

  private val advancedBaobabCode = """Config {
  lineLength: 40.0
  lineWidth: 2.0
  turningAngle: 25.0
  lineLengthMultiplier: 0.75
  lineWidthIncrement: 1.2
  maxIterations: 6
  startingColor: bark
}

Colors {
  bark: 0.45, 0.30, 0.15
  leaves: 0.25, 0.65, 0.30
}

Axiom: X

Rules {
  X -> <bark> F [ + X ] [ - X ] + F [ - X ]
  F -> F F
  X (0.15) -> <leaves> F [ + F ] [ - F ]
}"""

  private val neonHilbertMazeCode = """Config {
  lineLength: 52.0
  turningAngle: 90.0
  maxIterations: 7
  startingColor: cyan
}

Colors {
  cyan: 0.00, 1.00, 1.00
  magenta: 1.00, 0.00, 1.00
}

Axiom: A

Rules {
  A -> - <magenta> B F + <cyan> A F A + F B -
  B -> + <cyan> A F - <magenta> B F B - F A +
}"""

  private val springBlossomCode = """Config {
  turningAngle: 22.5
  maxIterations: 6
  lineLength: 150
}

Colors {
  wood: 0.6, 0.4, 0.2
  bloom: 1.0, 0.4, 0.7
}

Axiom: F

Rules {
  F (0.33) -> F [ + <bloom> F ] F
  F (0.33) -> F [ - <bloom> F ] F
  F (0.34) -> F <wood> F
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

  private val presetButtons = div(
    cls := "preset-buttons",
    h3("Przykłady:"),
    button(
      cls := "btn-preset",
      "Autumn Weed",
      onclick := { (_: dom.Event) => loadPreset("autumnWeed") }
    ),
    button(
      cls := "btn-preset",
      "Crystal Tree",
      onclick := { (_: dom.Event) => loadPreset("crystalTree") }
    ),
    button(
      cls := "btn-preset",
      "Weeping Willow",
      onclick := { (_: dom.Event) => loadPreset("weepingWillow") }
    ),
    button(
      cls := "btn-preset",
      "Advanced Baobab",
      onclick := { (_: dom.Event) => loadPreset("advancedBaobab") }
    ),
    button(
      cls := "btn-preset",
      "Neon Hilbert Maze",
      onclick := { (_: dom.Event) => loadPreset("neonHilbertMaze") }
    ),
    button(
      cls := "btn-preset",
      "Spring Blossom",
      onclick := { (_: dom.Event) => loadPreset("springBlossom") }
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
      case "autumnWeed" =>
        autumnWeedCode

      case "crystalTree" =>
        crystalTreeCode

      case "weepingWillow" =>
        weepingWillowCode

      case "advancedBaobab" =>
        advancedBaobabCode

      case "neonHilbertMaze" =>
        neonHilbertMazeCode

      case "springBlossom" =>
        springBlossomCode

      case _ =>
        defaultCode

    inputArea.value = code
    errorDiv.textContent = ""
    statusDiv.textContent = s"Załadowano preset: $presetName"
