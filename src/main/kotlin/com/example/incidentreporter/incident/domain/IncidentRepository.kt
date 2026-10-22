package com.example.incidentreporter.incident.domain

import java.time.Instant

interface IncidentRepository {
    fun insert(incident: Incident)
    fun findPage(page: Int, size: Int, query: String?, resolved: Boolean?): IncidentPage
    fun update(incident: Incident): Boolean
    fun setResolved(id: String, resolved: Boolean, at: Instant)
    fun delete(id: String): Boolean
    fun findById(id: String): Incident?
}

data class IncidentPage(
    val items: List<Incident>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)
