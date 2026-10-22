package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

internal data class IncidentUpdateBody(
    val title: String,
    val location: String,
    val description: String,
    val deletePhotoIds: List<String>,
)
