package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.document
import scalatags.JsDom.all._

object FractalApp:

  def main(args: Array[String]): Unit =
    dom.console.log("Fractala Frontend - Scala.js App Starting...")

    val apiBaseUrl = "http://localhost:9000"
    val apiService = new FractalApiService(apiBaseUrl)

    // Container that holds the currently active page (editor or examples).
    val contentContainer = div(id := "app-content").render

    // `router` is assigned below; `navigate` defers to it so views can be built first.
    var router: Router = null
    def navigate(path: String): Unit =
      if router != null then router.navigateTo(path)

    val (navBar, setActiveTab) = buildNavBar(navigate)

    def renderRoute(route: Route): Unit =
      setActiveTab(route)
      contentContainer.innerHTML = ""
      route match
        case Route.Editor =>
          contentContainer.appendChild(new EditorView(apiService, navigate).rootElement)
        case Route.Examples =>
          contentContainer.appendChild(new ExamplesView(apiService, navigate).rootElement)
        case Route.ExampleDetail(id) =>
          val editor = new EditorView(apiService, navigate)
          contentContainer.appendChild(editor.rootElement)
          editor.loadExample(id)

    router = new Router(renderRoute)

    document.body.innerHTML = ""
    document.body.appendChild(navBar)
    document.body.appendChild(contentContainer)

    router.start()
    dom.console.log("App initialized successfully.")

  /** Builds the top navigation bar and returns it together with a function that highlights the tab matching a given
    * route.
    */
  private def buildNavBar(navigate: String => Unit): (dom.html.Div, Route => Unit) =
    val editorTab = a(cls := "nav-tab", href := "/editor", "Editor").render
    editorTab.onclick = (event: dom.MouseEvent) =>
      event.preventDefault()
      navigate("/editor")

    val examplesTab = a(cls := "nav-tab", href := "/examples", "Examples").render
    examplesTab.onclick = (event: dom.MouseEvent) =>
      event.preventDefault()
      navigate("/examples")

    val bar = div(
      cls := "nav-bar",
      span(cls := "nav-brand", "Fractala"),
      div(cls := "nav-tabs", editorTab, examplesTab)
    ).render

    // The editor tab is active for every editor view, including deep-linked examples.
    def setActiveTab(route: Route): Unit =
      val editorActive = route != Route.Examples
      if editorActive then editorTab.classList.add("active") else editorTab.classList.remove("active")
      if editorActive then examplesTab.classList.remove("active") else examplesTab.classList.add("active")

    (bar, setActiveTab)
