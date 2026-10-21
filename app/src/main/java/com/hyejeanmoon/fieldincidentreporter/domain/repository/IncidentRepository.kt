package com.hyejeanmoon.fieldincidentreporter.domain.repository

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun getIncidents(): Flow<PagingData<Incident>>
    suspend fun getIncident(id: String): Incident
    suspend fun create(title: String, location: String, description: String, photoUris: List<String>): Incident
    suspend fun update(incident: Incident): Incident
    suspend fun delete(id: String)
}
