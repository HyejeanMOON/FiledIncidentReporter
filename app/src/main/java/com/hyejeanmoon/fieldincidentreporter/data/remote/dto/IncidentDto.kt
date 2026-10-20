package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

internal data class IncidentDto(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>?,
)
