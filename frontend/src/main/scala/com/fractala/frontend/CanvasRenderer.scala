package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.CanvasRenderingContext2D
import scala.scalajs.js

class CanvasRenderer(canvas: Canvas):
  private val ctx: CanvasRenderingContext2D =
    canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

  // Parametry skalowania
  private val scale = 0.05
  private val offsetX = canvas.width / 2.0
  private val offsetY = canvas.height * 0.8

  def clear(): Unit =
    ctx.clearRect(0, 0, canvas.width, canvas.height)

  def drawInstruction(instruction: DrawingInstruction): Unit =
    instruction match
      case DrawingInstruction.Line(line) =>
        drawLine(line)
      case DrawingInstruction.Dot(dot) =>
        drawDot(dot)
      case DrawingInstruction.Unknown(raw) =>
        dom.console.warn(s"[RENDERER] Nieobsługiwana instrukcja: $raw")

  private def drawLine(line: DrawLine): Unit =
    // Konwersja koloru ze Scali (0.0 - 1.0) na CSS (0 - 255)
    val r = Math.round(line.color.r * 255).toInt
    val g = Math.round(line.color.g * 255).toInt
    val b = Math.round(line.color.b * 255).toInt

    ctx.beginPath()

    // Przeliczenie współrzędnych z odwróceniem osi Y
    val startX = line.xFrom * scale + offsetX
    val startY = line.yFrom * -scale + offsetY
    ctx.moveTo(startX, startY)

    val endX = line.xTo * scale + offsetX
    val endY = line.yTo * -scale + offsetY
    ctx.lineTo(endX, endY)

    ctx.strokeStyle = s"rgb($r, $g, $b)"
    ctx.lineWidth = line.lineWidth
    ctx.stroke()

  private def drawDot(dot: DrawDot): Unit =
    ctx.beginPath()

    // Przeliczenie współrzędnych
    val centerX = dot.x * scale + offsetX
    val centerY = dot.y * -scale + offsetY

    // Rysowanie kropki (koła)
    ctx.arc(centerX, centerY, dot.radius * scale, 0, 2 * Math.PI)

    // Domyślny kolor wypełnienia
    ctx.fillStyle = "#ffaa00"
    ctx.fill()

  def setScale(newScale: Double): CanvasRenderer =
    new CanvasRenderer(canvas)

  def resize(width: Int, height: Int): Unit =
    canvas.width = width
    canvas.height = height