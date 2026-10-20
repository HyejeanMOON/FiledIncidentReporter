package com.hyejeanmoon.fieldincidentreporter.domain.repository

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

interface IncidentRepository {
    suspend fun getIncidents(): List<Incident>
    suspend fun create(title: String, location: String, description: String, photoUris: List<String>): Incident
    suspend fun update(incident: Incident): Incident
    suspend fun delete(id: String)
}
