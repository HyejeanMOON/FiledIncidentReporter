package com.hyejeanmoon.fieldincidentreporter.domain.model

data class IncidentSummary(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoPreviews: List<String>,
)
