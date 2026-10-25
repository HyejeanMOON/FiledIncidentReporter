package com.example.incidentreporter.incident.domain

import java.time.Instant

/** A place in the list order: newest first, with `createdAt` ties broken by the higher `id`. */
data class IncidentPosition(val createdAt: Instant, val id: String)

/** [next] is the position of the last item when more incidents follow it, or null on the last page. */
data class IncidentPage(
    val items: List<Incident>,
    val next: IncidentPosition?,
)
