package com.fractala.frontend

import io.circe._
import io.circe.generic.semiauto._

case class Color(r: Double, g: Double, b: Double)

object Color:
  given Decoder[Color] = deriveDecoder[Color]

case class DrawLine(
    xFrom: Double,
    yFrom: Double,
    xTo: Double,
    yTo: Double,
    color: Color,
    lineWidth: Double
)

object DrawLine:
  given Decoder[DrawLine] = deriveDecoder[DrawLine]

case class DrawDot(
    x: Double,
    y: Double,
    radius: Double
)

object DrawDot:
  given Decoder[DrawDot] = deriveDecoder[DrawDot]

enum DrawingInstruction:
  case Line(data: DrawLine)
  case Dot(data: DrawDot)
  case Unknown(raw: String)

object DrawingInstruction:
  given Decoder[DrawingInstruction] = (c: HCursor) =>
    c.as[JsonObject].flatMap { obj =>
      obj.toMap.headOption match
        case Some(("DrawLine", value)) =>
          value.as[DrawLine].map(DrawingInstruction.Line(_))
        case Some(("DrawDot", value)) =>
          value.as[DrawDot].map(DrawingInstruction.Dot(_))
        case other =>
          Right(DrawingInstruction.Unknown(obj.toString))
    }

case class FractalRequest(code: String)

object FractalRequest:
  given Encoder[FractalRequest] = deriveEncoder[FractalRequest]

/** A single example fractal as returned by the catalog API (`GET /fractals/{id}`). */
case class ExampleFractal(
    id: String,
    name: String,
    description: String,
    code: String,
    imageUrl: String
)

object ExampleFractal:
  given Decoder[ExampleFractal] = deriveDecoder[ExampleFractal]

/** A page of the paginated catalog (`GET /fractals`). The `meta` field is ignored. */
case class FractalsPage(items: List[ExampleFractal])

object FractalsPage:
  given Decoder[FractalsPage] = deriveDecoder[FractalsPage]
