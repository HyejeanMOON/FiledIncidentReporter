package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class IncidentDto(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photos: List<IncidentPhotoDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val resolvedAt: String? = null,
)

@Serializable
internal data class IncidentPhotoDto(
    val id: String,
    val url: String,
    val thumbnailUrl: String = "",
)

/** [nextCursor] points just after the last of [items] and is null on the last page. */
@Serializable
internal data class IncidentPageDto(
    val items: List<IncidentDto>,
    val nextCursor: String? = null,
)

@Serializable
internal data class IncidentBody(
    val title: String,
    val location: String,
    val description: String,
    val deletePhotoIds: List<String>? = null,
)
