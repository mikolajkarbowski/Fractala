package com.fractala.api.models.responses

/** Standard RFC 7807 (Problem Details for HTTP APIs)
  */
case class ErrorResponse(
    `type`: String = "about:blank",
    title: String,
    status: Int,
    detail: String,
    instance: Option[String] = None
)

object ErrorResponse {

  def notFound(detail: String, instance: Option[String] = None): ErrorResponse =
    ErrorResponse(
      title = "Not Found",
      status = 404,
      detail = detail,
      instance = instance
    )

  def badRequest(
      detail: String,
      instance: Option[String] = None
  ): ErrorResponse =
    ErrorResponse(
      title = "Bad Request",
      status = 400,
      detail = detail,
      instance = instance
    )

  def unprocessableEntity(
      detail: String,
      instance: Option[String] = None
  ): ErrorResponse =
    ErrorResponse(
      title = "Unprocessable Entity",
      status = 422,
      detail = detail,
      instance = instance
    )

  def internalServerError(
      detail: String,
      instance: Option[String] = None
  ): ErrorResponse =
    ErrorResponse(
      title = "Internal Server Error",
      status = 500,
      detail = detail,
      instance = instance
    )

  def tooManyRequests(
      detail: String,
      instance: Option[String] = None
  ): ErrorResponse =
    ErrorResponse(
      title = "Too Many Requests",
      status = 429,
      detail = detail,
      instance = instance
    )
}
