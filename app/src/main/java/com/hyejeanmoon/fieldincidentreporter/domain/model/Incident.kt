package com.hyejeanmoon.fieldincidentreporter.domain.model

import java.time.Instant

data class Incident(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photos: List<IncidentPhoto> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isEdited: Boolean get() = updatedAt != createdAt
}

data class IncidentPage(
    val items: List<Incident>,
    val page: Int,
    val hasNext: Boolean,
)

/** A successful write, published by the repository so that every screen can stay in sync. */
sealed interface IncidentChange {
    data class Saved(val incident: Incident) : IncidentChange
    data class Deleted(val id: String) : IncidentChange
}
