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
    /** Set when an operator resolves the report in the admin dashboard. */
    val resolvedAt: Instant? = null,
) {
    val isEdited: Boolean get() = updatedAt != createdAt

    val isResolved: Boolean get() = resolvedAt != null
}
