package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

internal data class IncidentUpdateBody(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val retainedPhotoUris: List<String>,
)
