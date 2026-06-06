package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.CanvasRenderingContext2D

/** Renders drawing instructions onto the canvas.
  *
  * Instructions are drawn as they stream in, while the whole structure is always kept visible: whenever a new
  * instruction would fall outside the canvas, the view is rescaled.
  *
  * Each rescale fits the current content into only [[streamFillFraction]] of the canvas, leaving headroom to grow
  * before another rescale is needed. When the stream ends, the view settles into a clean [[finalFillFraction]] fit.
  */
class CanvasRenderer(canvas: Canvas):
  private val ctx: CanvasRenderingContext2D =
    canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

  // Final fit: the fractal occupies this fraction of the canvas (the rest is padding).
  private val finalFillFraction = 0.95
  // Streaming fit: each rescale shrinks content to this smaller fraction.
  private val streamFillFraction = 0.95

  private final case class Transform(scale: Double, centerX: Double, centerY: Double):
    def tx(x: Double): Double = canvas.width / 2.0 + (x - centerX) * scale
    // Y is flipped so the fractal grows upward, matching the generator's coordinate system.
    def ty(y: Double): Double = canvas.height / 2.0 - (y - centerY) * scale

  private final case class Bounds(minX: Double, minY: Double, maxX: Double, maxY: Double)

  // --- Streaming state ---
  private val streamed = scala.collection.mutable.ArrayBuffer.empty[DrawingInstruction]
  private var minX = Double.MaxValue
  private var minY = Double.MaxValue
  private var maxX = Double.MinValue
  private var maxY = Double.MinValue
  private var hasContent = false
  private var transform: Option[Transform] = None

  def clear(): Unit =
    ctx.clearRect(0, 0, canvas.width, canvas.height)

  /** Starts a new streaming render: resets all state and clears the canvas. */
  def beginStream(): Unit =
    streamed.clear()
    minX = Double.MaxValue
    minY = Double.MaxValue
    maxX = Double.MinValue
    maxY = Double.MinValue
    hasContent = false
    transform = None
    clear()

  /** Adds one instruction: drawn immediately if it fits the current view, otherwise the view is rescaled to fit
    * everything and all instructions so far are redrawn.
    */
  def addInstruction(instruction: DrawingInstruction): Unit =
    streamed += instruction
    includeInstruction(instruction)
    transform match
      case Some(t) if fitsWithin(instruction, t) => draw(instruction, t)
      case _                                     => rescaleAndRedraw(streamFillFraction)

  /** Ends the streaming render with a clean, centered fit of the whole fractal. */
  def endStream(): Unit =
    rescaleAndRedraw(finalFillFraction)

  private def rescaleAndRedraw(fillFraction: Double): Unit =
    currentBounds.foreach { bounds =>
      val t = transformFor(bounds, fillFraction)
      transform = Some(t)
      clear()
      streamed.foreach(draw(_, t))
    }

  private def currentBounds: Option[Bounds] =
    if hasContent then Some(Bounds(minX, minY, maxX, maxY)) else None

  private def includeInstruction(instruction: DrawingInstruction): Unit =
    instruction match
      case DrawingInstruction.Line(line) =>
        includePoint(line.xFrom, line.yFrom)
        includePoint(line.xTo, line.yTo)
      case DrawingInstruction.Dot(dot) =>
        includePoint(dot.x - dot.radius, dot.y - dot.radius)
        includePoint(dot.x + dot.radius, dot.y + dot.radius)
      case DrawingInstruction.Unknown(_) =>
        ()

  private def includePoint(x: Double, y: Double): Unit =
    hasContent = true
    if x < minX then minX = x
    if y < minY then minY = y
    if x > maxX then maxX = x
    if y > maxY then maxY = y

  /** True if drawing the instruction under the given transform stays within the canvas. */
  private def fitsWithin(instruction: DrawingInstruction, t: Transform): Boolean =
    def inside(x: Double, y: Double): Boolean =
      val px = t.tx(x)
      val py = t.ty(y)
      px >= 0 && px <= canvas.width && py >= 0 && py <= canvas.height
    instruction match
      case DrawingInstruction.Line(line) =>
        inside(line.xFrom, line.yFrom) && inside(line.xTo, line.yTo)
      case DrawingInstruction.Dot(dot) =>
        inside(dot.x - dot.radius, dot.y - dot.radius) && inside(dot.x + dot.radius, dot.y + dot.radius)
      case DrawingInstruction.Unknown(_) =>
        true

  private def transformFor(bounds: Bounds, fillFraction: Double): Transform =
    val contentWidth = bounds.maxX - bounds.minX
    val contentHeight = bounds.maxY - bounds.minY
    val availableWidth = canvas.width * fillFraction
    val availableHeight = canvas.height * fillFraction

    // Scale to fit whichever dimension is the binding constraint; a zero-size dimension imposes none.
    val scaleX = if contentWidth > 1e-9 then availableWidth / contentWidth else Double.PositiveInfinity
    val scaleY = if contentHeight > 1e-9 then availableHeight / contentHeight else Double.PositiveInfinity
    val scale = math.min(scaleX, scaleY) match
      case s if s.isInfinite || s.isNaN => 1.0 // degenerate case: a single point
      case s                            => s

    Transform(scale, (bounds.minX + bounds.maxX) / 2.0, (bounds.minY + bounds.maxY) / 2.0)

  private def draw(instruction: DrawingInstruction, t: Transform): Unit =
    instruction match
      case DrawingInstruction.Line(line) =>
        drawLine(line, t)
      case DrawingInstruction.Dot(dot) =>
        drawDot(dot, t)
      case DrawingInstruction.Unknown(raw) =>
        dom.console.warn(s"[RENDERER] Unsupported instruction: $raw")

  private def drawLine(line: DrawLine, t: Transform): Unit =
    val r = Math.round(line.color.r * 255).toInt
    val g = Math.round(line.color.g * 255).toInt
    val b = Math.round(line.color.b * 255).toInt

    ctx.beginPath()
    ctx.moveTo(t.tx(line.xFrom), t.ty(line.yFrom))
    ctx.lineTo(t.tx(line.xTo), t.ty(line.yTo))
    ctx.strokeStyle = s"rgb($r, $g, $b)"
    ctx.lineWidth = line.lineWidth
    ctx.stroke()

  private def drawDot(dot: DrawDot, t: Transform): Unit =
    ctx.beginPath()
    ctx.arc(t.tx(dot.x), t.ty(dot.y), dot.radius * t.scale, 0, 2 * Math.PI)
    ctx.fillStyle = "#ffaa00"
    ctx.fill()
