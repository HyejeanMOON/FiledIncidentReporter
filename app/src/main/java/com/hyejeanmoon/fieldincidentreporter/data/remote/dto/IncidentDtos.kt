package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

internal data class IncidentDto(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photos: List<IncidentPhotoDto>?,
    val createdAt: String,
    val updatedAt: String,
)

internal data class IncidentPhotoDto(
    val id: String,
    val url: String,
    val thumbnailUrl: String,
)

internal data class IncidentPageDto(
    val items: List<IncidentDto>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

internal data class IncidentBody(
    val title: String,
    val location: String,
    val description: String,
    val deletePhotoIds: List<String>? = null,
)
