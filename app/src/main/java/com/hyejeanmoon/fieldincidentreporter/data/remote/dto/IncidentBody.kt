package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

internal data class IncidentBody(
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
)
