package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.Canvas
import scalatags.JsDom.all._

object FractalApp:

  def main(args: Array[String]): Unit =
    dom.console.log("Fractal Frontend - Scala.js App Starting...")

    val apiUrl = "http://localhost:9000/fractals/render"

    val canvas = createCanvas()

    val renderer = new CanvasRenderer(canvas)
    val apiService = new FractalApiService(apiUrl)
    val ui = new FractalUI(renderer, apiService)

    val rightPanel = div(
      cls := "right-panel",
      canvas
    ).render

    val appContainer = div(
      cls := "app-container",
      ui.leftPanel,
      rightPanel
    ).render

    document.body.innerHTML = ""
    document.body.appendChild(appContainer)

    dom.console.log("Aplikacja załadowana")

  private def createCanvas(): Canvas =
    val canvas = document.createElement("canvas").asInstanceOf[Canvas]
    canvas.id = "fractalCanvas"
    canvas.width = 800
    canvas.height = 800
    canvas
