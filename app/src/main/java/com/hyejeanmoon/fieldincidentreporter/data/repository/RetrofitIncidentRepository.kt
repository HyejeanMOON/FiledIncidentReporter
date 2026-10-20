package com.hyejeanmoon.fieldincidentreporter.data.repository

import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.incidentRequest
import com.hyejeanmoon.fieldincidentreporter.data.toDto
import com.hyejeanmoon.fieldincidentreporter.data.toIncident
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import retrofit2.HttpException

internal class RetrofitIncidentRepository(private val api: IncidentApi) : IncidentRepository {
    override suspend fun getIncidents() = incidentRequest {
        api.getIncidents().map { it.toIncident() }
    }

    override suspend fun create(
        title: String,
        location: String,
        description: String,
        photoUris: List<String>,
    ): Incident = incidentRequest {
        api.create(IncidentBody(title, location, description, photoUris)).toIncident()
    }

    override suspend fun update(incident: Incident) = incidentRequest {
        api.update(incident.id, incident.toDto()).toIncident()
    }

    override suspend fun delete(id: String) = incidentRequest {
        val response = api.delete(id)
        if (!response.isSuccessful) throw HttpException(response)
    }
}
