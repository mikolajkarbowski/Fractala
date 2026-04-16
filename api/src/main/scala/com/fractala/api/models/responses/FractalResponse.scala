package com.fractala.api.models.responses
import java.util.UUID

case class FractalResponse(
    id: UUID,
    name: String,
    description: String,
    code: String,
    imageUrl: String
)
