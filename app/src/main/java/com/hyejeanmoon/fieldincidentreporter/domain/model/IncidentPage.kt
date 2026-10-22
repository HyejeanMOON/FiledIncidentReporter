package com.hyejeanmoon.fieldincidentreporter.domain.model

data class IncidentPage(
    val items: List<IncidentSummary>,
    val page: Int,
    val hasNext: Boolean,
)
