package com.hyejeanmoon.fieldincidentreporter.domain.model

data class Incident(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String> = emptyList(),
)
