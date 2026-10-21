package com.example.incidentreporter.incident.domain

interface IncidentRepository {
    fun insert(incident: Incident)
    fun findPage(page: Int, size: Int): IncidentPage
}

data class IncidentPage(
    val items: List<Incident>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)
