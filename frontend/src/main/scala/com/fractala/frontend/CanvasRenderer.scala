package com.fractala.frontend

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.CanvasRenderingContext2D
import scala.scalajs.js

class CanvasRenderer(canvas: Canvas):
  private val ctx: CanvasRenderingContext2D =
    canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

  // Scaling parameters
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
        dom.console.warn(s"[RENDERER] Unsupported instruction: $raw")

  private def drawLine(line: DrawLine): Unit =
    // Convert color from Scala (0.0 - 1.0) to CSS (0 - 255)
    val r = Math.round(line.color.r * 255).toInt
    val g = Math.round(line.color.g * 255).toInt
    val b = Math.round(line.color.b * 255).toInt

    ctx.beginPath()

    // Map coordinates, flipping the Y axis
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

    // Map coordinates
    val centerX = dot.x * scale + offsetX
    val centerY = dot.y * -scale + offsetY

    // Draw the dot (circle)
    ctx.arc(centerX, centerY, dot.radius * scale, 0, 2 * Math.PI)

    // Default fill color
    ctx.fillStyle = "#ffaa00"
    ctx.fill()

  def resize(width: Int, height: Int): Unit =
    canvas.width = width
    canvas.height = height
