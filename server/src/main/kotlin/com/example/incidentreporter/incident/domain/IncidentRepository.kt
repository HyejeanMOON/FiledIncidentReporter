package com.example.incidentreporter.incident.domain

import java.time.Instant

interface IncidentRepository {
    fun insert(incident: Incident)
    /** Skips [offset] incidents after the [after] position, or from the top when it is null. */
    fun findPage(after: IncidentPosition?, offset: Long, size: Int, query: String?, resolved: Boolean?): IncidentPage
    fun update(incident: Incident): Boolean
    fun setResolved(id: String, resolved: Boolean, at: Instant)
    fun delete(id: String): Boolean
    fun findById(id: String): Incident?
}

/** A place in the list order: newest first, with `createdAt` ties broken by the higher `id`. */
data class IncidentPosition(val createdAt: Instant, val id: String)

/** [next] is the position of the last item when more incidents follow it, or null on the last page. */
data class IncidentPage(
    val items: List<Incident>,
    val next: IncidentPosition?,
)
