package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

internal data class IncidentPageDto(
    val items: List<IncidentListItemDto>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

internal data class IncidentListItemDto(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photos: List<IncidentPhotoDto>,
)
