package com.fractala.frontend

import org.scalajs.dom

/** Application routes, backed by clean (History API) URLs. */
enum Route:
  case Editor
  case Examples
  case ExampleDetail(id: String)

object Route:
  /** Parses a URL path (e.g. "/examples/abc") into a [[Route]]. Unknown paths fall back to [[Editor]]. */
  def fromPath(path: String): Route =
    path.split("/").filter(_.nonEmpty).toList match
      case Nil                     => Route.Editor
      case "editor" :: Nil         => Route.Editor
      case "examples" :: Nil       => Route.Examples
      case "examples" :: id :: Nil => Route.ExampleDetail(id)
      case _                       => Route.Editor

/** Minimal client-side router using the History API. Calls `onRoute` whenever the active route changes. */
class Router(onRoute: Route => Unit):

  /** Starts listening for browser back/forward navigation and renders the current route. */
  def start(): Unit =
    dom.window.addEventListener("popstate", (_: dom.Event) => renderCurrent())
    renderCurrent()

  /** Pushes a new URL onto the history stack and renders the matching route. */
  def navigateTo(path: String): Unit =
    dom.window.history.pushState(null, "", path)
    renderCurrent()

  private def renderCurrent(): Unit =
    onRoute(Route.fromPath(dom.window.location.pathname))
