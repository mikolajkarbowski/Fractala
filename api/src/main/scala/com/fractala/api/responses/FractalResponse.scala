package com.fractala.api.responses
import java.util.UUID

case class FractalResponse(
    id: UUID,
    name: String,
    description: String,
    code: String,
    imageUrl: String
)
