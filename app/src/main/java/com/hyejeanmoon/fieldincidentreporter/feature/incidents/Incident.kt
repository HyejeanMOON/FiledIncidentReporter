package com.hyejeanmoon.fieldincidentreporter.feature.incidents

data class Incident(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String> = emptyList(),
)

data class IncidentUiState(
    val incidents: List<Incident> = emptyList(),
)
