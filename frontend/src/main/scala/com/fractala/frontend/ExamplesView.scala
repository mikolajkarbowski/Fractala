package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.html.Div
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** The examples page: an image gallery of catalog fractals fetched from the API.
  *
  * @param apiService
  *   used to fetch the catalog (`GET /fractals`).
  * @param navigate
  *   navigates to an example's editor URL when a card is clicked.
  */
class ExamplesView(apiService: FractalApiService, navigate: String => Unit):

  private val statusArea = div(cls := "examples-status", "Loading examples...").render
  private val grid = div(cls := "examples-grid").render

  val rootElement: Div = div(
    cls := "examples-page",
    h2("Example Fractals"),
    p(cls := "examples-subtitle", "Choose a fractal to open it in the editor."),
    statusArea,
    grid
  ).render

  loadExamples()

  private def loadExamples(): Unit =
    apiService.fetchExamples().foreach {
      case Right(items) if items.isEmpty =>
        statusArea.textContent = "No examples available."
      case Right(items) =>
        statusArea.style.display = "none"
        items.foreach(example => grid.appendChild(buildCard(example)))
      case Left(error) =>
        statusArea.textContent = error
        statusArea.classList.add("examples-error")
    }

  private def buildCard(example: ExampleFractal): Div =
    val card = div(
      cls := "example-card",
      onclick := { (_: dom.Event) => navigate(s"/examples/${example.id}") },
      div(
        cls := "example-card-image",
        img(cls := "example-card-img", src := example.imageUrl, alt := example.name)
      ),
      div(
        cls := "example-card-body",
        h3(cls := "example-card-title", example.name),
        p(cls := "example-card-desc", example.description)
      )
    ).render

    // Fall back to a placeholder if the preview image fails to load.
    val imgEl = card.querySelector(".example-card-img").asInstanceOf[dom.html.Image]
    val imageBox = card.querySelector(".example-card-image")
    imgEl.addEventListener(
      "error",
      (_: dom.Event) =>
        imgEl.style.display = "none"
        imageBox.classList.add("no-image")
    )
    card
