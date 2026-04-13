package com.fractala.api.models.responses

private case class PaginationMetadata(
    totalCount: Int,
    limit: Int,
    offset: Int,
    totalPages: Int,
    hasNextPage: Boolean,
    hasPreviousPage: Boolean
)

case class PaginatedResponse[A](
    items: List[A],
    meta: PaginationMetadata
)

object PaginatedResponse {

  def fromList[A](
      allItems: List[A],
      limit: Int,
      offset: Int
  ): PaginatedResponse[A] = {
    val safeLimit = if (limit > 0) limit else 10
    val safeOffset = if (offset >= 0) offset else 0
    val totalCount = allItems.size
    val paginatedItems = allItems.drop(safeOffset).take(safeLimit)
    val totalPages = math.ceil(totalCount.toDouble / safeLimit).toInt
    val hasNextPage = (safeOffset + safeLimit) < totalCount

    val meta = PaginationMetadata(
      totalCount = totalCount,
      limit = safeLimit,
      offset = safeOffset,
      totalPages = totalPages,
      hasNextPage = hasNextPage,
      hasPreviousPage = safeOffset > 0
    )

    PaginatedResponse(paginatedItems, meta)
  }
}
