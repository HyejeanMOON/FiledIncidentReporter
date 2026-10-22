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
)

@Serializable
internal data class IncidentPhotoDto(
    val id: String,
    val url: String,
    val thumbnailUrl: String,
)

@Serializable
internal data class IncidentPageDto(
    val items: List<IncidentDto>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

@Serializable
internal data class IncidentBody(
    val title: String,
    val location: String,
    val description: String,
    val deletePhotoIds: List<String>? = null,
)
