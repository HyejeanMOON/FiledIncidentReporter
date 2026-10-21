package com.example.incidentreporter.incident.domain

interface IncidentRepository {
    fun insert(incident: Incident)
}
