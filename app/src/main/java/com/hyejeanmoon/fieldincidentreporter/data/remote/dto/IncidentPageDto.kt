package com.hyejeanmoon.fieldincidentreporter.data.remote.dto

internal data class IncidentPageDto(
    val items: List<IncidentDto>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)
