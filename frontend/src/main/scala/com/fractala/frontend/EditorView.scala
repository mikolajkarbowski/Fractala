package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Anchor, Canvas, Div, TextArea}
import scalatags.JsDom.all._
import scala.scalajs.js
import scala.concurrent.{Future, Promise}
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

  // Approximate line height in px (font-size 14px * line-height 1.5), used to scroll errors into view.
  private val lineHeightPx = 21.0

  // localStorage key under which the editor contents are persisted across reloads.
  private val storageKey = "fractala.editorCode"

  /** Returns the persisted editor code, or the default snippet if nothing is saved. */
  private def loadInitialCode(): String =
    try
      val saved = dom.window.localStorage.getItem(storageKey)
      if saved != null && saved.nonEmpty then saved else defaultCode
    catch case _: Throwable => defaultCode // localStorage may be unavailable (e.g. private mode)

  /** Persists the current editor contents to localStorage. */
  private def saveCode(): Unit =
    try dom.window.localStorage.setItem(storageKey, inputArea.value)
    catch case _: Throwable => ()

  private val canvas: Canvas =
    val c = document.createElement("canvas").asInstanceOf[Canvas]
    c.id = "fractalCanvas"
    c.width = 800
    c.height = 800
    c

  private val renderer = new CanvasRenderer(canvas)

  private var isRendering = false

  private val errorDiv = div(id := "errorMessage", cls := "error-msg").render

  // Backdrop layer behind the textarea, used to paint a coloured highlight under the error location.
  private val highlightsDiv = div(cls := "editor-highlights").render

  private val inputArea: TextArea = textarea(
    id := "dslInput",
    cls := "code-editor",
    spellcheck := false,
    loadInitialCode()
  ).render

  // Keep the highlight backdrop scrolled in sync with the textarea, and clear the error + persist on edit.
  inputArea.addEventListener("scroll", (_: dom.Event) => syncHighlightScroll())
  inputArea.addEventListener(
    "input",
    (_: dom.Event) =>
      clearError()
      saveCode()
  )

  private val editorWrap = div(cls := "editor-wrap", highlightsDiv, inputArea).render

  private val copyButton = button(
    cls := "btn-secondary",
    "Copy code",
    onclick := { (_: dom.Event) => copyCode() }
  ).render

  private val clearButton = button(
    cls := "btn-secondary",
    "Clear",
    onclick := { (_: dom.Event) => clearCode() }
  ).render

  private val saveButton = button(
    cls := "btn-secondary",
    "Save PNG",
    onclick := { (_: dom.Event) => saveImage() }
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
    editorWrap,
    div(cls := "editor-toolbar", copyButton, clearButton),
    sendButton,
    statusDiv
  ).render

  private val rightPanel = div(
    cls := "right-panel",
    div(
      cls := "canvas-panel",
      div(cls := "canvas-toolbar", saveButton),
      canvas
    )
  ).render

  val rootElement: Div = div(cls := "app-container", leftPanel, rightPanel).render

  /** Fetches an example by id, loads its code into the editor, and renders it automatically. */
  def loadExample(id: String): Unit =
    clearError()
    statusDiv.textContent = "Loading example..."
    apiService.fetchExample(id).foreach {
      case Right(example) =>
        inputArea.value = example.code
        saveCode()
        statusDiv.textContent = s"Loaded example: ${example.name}"
        handleRender()
      case Left(error) =>
        statusDiv.textContent = ""
        showError(RenderError("Could not load example", error, None, None))
    }

  private def handleRender(): Unit =
    if isRendering then return

    val code = inputArea.value
    clearError()
    statusDiv.textContent = ""
    isRendering = true
    sendButton.disabled = true
    sendButton.textContent = "Drawing..."

    renderer.beginStream()
    var instructionCount = 0

    apiService.renderFractal(
      code = code,
      onInstruction = { instruction =>
        instructionCount += 1
        renderer.addInstruction(instruction)
        if instructionCount % 100 == 0 then statusDiv.textContent = s"Drawn $instructionCount instructions..."
      },
      onError = { error =>
        showError(error)
        statusDiv.textContent = ""
        resetButton()
      },
      onComplete = { () =>
        renderer.endStream()
        statusDiv.textContent = s"Done! Drawn $instructionCount instructions."
        resetButton()
      }
    )

  private def resetButton(): Unit =
    isRendering = false
    sendButton.disabled = false
    sendButton.textContent = "Generate"

  /** Copies the entire editor contents to the clipboard, with a graceful fallback for older browsers. */
  private def copyCode(): Unit =
    val text = inputArea.value
    val clipboard = dom.window.navigator.asInstanceOf[js.Dynamic].clipboard
    if !js.isUndefined(clipboard) && clipboard != null then
      clipboard.writeText(text)
      flashCopied()
    else
      inputArea.focus()
      inputArea.select()
      dom.document.asInstanceOf[js.Dynamic].execCommand("copy")
      flashCopied()

  private def flashCopied(): Unit =
    copyButton.textContent = "Copied!"
    dom.window.setTimeout(() => copyButton.textContent = "Copy code", 1500)

  /** Removes all text from the editor. */
  private def clearCode(): Unit =
    inputArea.value = ""
    saveCode()
    clearError()
    inputArea.focus()

  /** Saves the current canvas as a PNG. Uses the File System Access API (a native "Save As" dialog where the user picks
    * the name and location) when the browser supports it, and falls back to a direct download otherwise (e.g.
    * Firefox/Safari).
    */
  private def saveImage(): Unit =
    val win = dom.window.asInstanceOf[js.Dynamic]
    if js.isUndefined(win.showSaveFilePicker) then saveImageWithDownload()
    else saveImageWithDialog(win)

  private def saveImageWithDialog(win: js.Dynamic): Unit =
    val accept = js.Dynamic.literal()
    accept.updateDynamic("image/png")(js.Array(".png"))
    val options = js.Dynamic.literal(
      suggestedName = "fractal.png",
      types = js.Array(js.Dynamic.literal(description = "PNG image", accept = accept))
    )

    val saveOp =
      for
        handle <- win.showSaveFilePicker(options).asInstanceOf[js.Promise[js.Dynamic]].toFuture
        blob <- canvasToBlob()
        writable <- handle.createWritable().asInstanceOf[js.Promise[js.Dynamic]].toFuture
        _ <- writable.write(blob.asInstanceOf[js.Any]).asInstanceOf[js.Promise[js.Any]].toFuture
        _ <- writable.close().asInstanceOf[js.Promise[js.Any]].toFuture
      yield ()

    // The user cancelling the dialog rejects with an AbortError; just log and move on.
    saveOp.recover { case error => dom.console.log(s"[SAVE] Cancelled or failed: ${error.getMessage}") }

  private def canvasToBlob(): Future[dom.Blob] =
    val promise = Promise[dom.Blob]()
    val callback: js.Function1[dom.Blob, Unit] = blob =>
      if blob != null then promise.success(blob)
      else promise.failure(new RuntimeException("Could not export the canvas."))
    canvas.asInstanceOf[js.Dynamic].toBlob(callback, "image/png")
    promise.future

  private def saveImageWithDownload(): Unit =
    val dataUrl = canvas.toDataURL("image/png")
    val link = document.createElement("a").asInstanceOf[Anchor]
    link.href = dataUrl
    link.asInstanceOf[js.Dynamic].download = "fractal.png"
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

  /** Renders a formatted error block and highlights the offending location in the editor. */
  private def showError(error: RenderError): Unit =
    errorDiv.innerHTML = ""
    errorDiv.appendChild(div(cls := "error-title", error.summary).render)
    (error.line, error.column) match
      case (Some(l), Some(c)) => errorDiv.appendChild(div(cls := "error-loc", s"Line $l, column $c").render)
      case (Some(l), None)    => errorDiv.appendChild(div(cls := "error-loc", s"Line $l").render)
      case _                  => ()
    errorDiv.appendChild(pre(cls := "error-detail", error.detail).render)

    error.line match
      case Some(l) => highlightError(l, error.column.getOrElse(1))
      case None    => clearHighlight()

  private def clearError(): Unit =
    errorDiv.innerHTML = ""
    clearHighlight()

  private def clearHighlight(): Unit =
    highlightsDiv.innerHTML = ""

  /** Paints a highlight on the given 1-based line, starting at the given 1-based column. */
  private def highlightError(line: Int, column: Int): Unit =
    val text = inputArea.value
    val lines = text.split("\n", -1)
    val idx = line - 1
    if idx < 0 || idx >= lines.length then clearHighlight()
    else
      val lineText = lines(idx)
      val lineStart = lines.take(idx).map(_.length + 1).sum
      val startCol = math.min(math.max(column - 1, 0), lineText.length)
      // Highlight from the error column to the end of the line; if the column is at/after the
      // line end, highlight the whole line instead.
      val (from, to) =
        if startCol >= lineText.length then (0, lineText.length)
        else (startCol, lineText.length)

      val globalFrom = lineStart + from
      val globalTo = lineStart + to
      val before = text.substring(0, globalFrom)
      val highlighted = text.substring(globalFrom, globalTo)
      val after = text.substring(globalTo)

      highlightsDiv.innerHTML = ""
      highlightsDiv.appendChild(document.createTextNode(before))
      if highlighted.nonEmpty then
        val span = document.createElement("span")
        span.setAttribute("class", "error-highlight")
        span.appendChild(document.createTextNode(highlighted))
        highlightsDiv.appendChild(span)
      highlightsDiv.appendChild(document.createTextNode(after))

      scrollLineIntoView(line)

  private def scrollLineIntoView(line: Int): Unit =
    val target = (line - 1) * lineHeightPx
    val viewTop = inputArea.scrollTop
    val viewHeight = inputArea.clientHeight.toDouble
    if target < viewTop || target > viewTop + viewHeight - lineHeightPx then
      inputArea.scrollTop = math.max(0.0, target - viewHeight / 2)
    syncHighlightScroll()

  private def syncHighlightScroll(): Unit =
    highlightsDiv.scrollTop = inputArea.scrollTop
    highlightsDiv.scrollLeft = inputArea.scrollLeft
