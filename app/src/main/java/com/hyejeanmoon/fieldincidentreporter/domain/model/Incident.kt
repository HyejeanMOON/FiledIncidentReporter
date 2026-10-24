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
    /**
     * Whether [updatedAt] records a change of its own. The server also sets it when the report is
     * created and when it is resolved, and [createdAt] and [resolvedAt] already show those times.
     */
    val hasSeparateUpdateTime: Boolean get() = updatedAt != createdAt && updatedAt != resolvedAt

    val isResolved: Boolean get() = resolvedAt != null
}
