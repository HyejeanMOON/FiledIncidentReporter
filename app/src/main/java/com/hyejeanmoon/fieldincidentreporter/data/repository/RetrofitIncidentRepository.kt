package com.hyejeanmoon.fieldincidentreporter.data.repository

import android.content.Context
import android.net.Uri
import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.incidentRequest
import com.hyejeanmoon.fieldincidentreporter.data.remote.photoPart
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncident
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentUpdateBody
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import javax.inject.Inject

internal class RetrofitIncidentRepository @Inject constructor(
    private val api: IncidentApi,
    @ApplicationContext private val context: Context,
) : IncidentRepository {
    override suspend fun getIncidents() = incidentRequest {
        api.getIncidents().map { it.toIncident() }
    }

    override suspend fun create(
        title: String,
        location: String,
        description: String,
        photoUris: List<String>,
    ): Incident = incidentRequest {
        api.create(
            IncidentBody(title, location, description),
            photoUris.mapIndexed { index, uri -> photoPart(context, uri, index) },
        ).toIncident()
    }

    override suspend fun update(incident: Incident) = incidentRequest {
        val (localPhotos, retainedPhotos) = incident.photoUris.partition { uri ->
            Uri.parse(uri).scheme in setOf("content", "file")
        }
        api.update(
            incident.id,
            IncidentUpdateBody(
                incident.id,
                incident.title,
                incident.location,
                incident.description,
                retainedPhotos,
            ),
            localPhotos.mapIndexed { index, uri -> photoPart(context, uri, index) },
        ).toIncident()
    }

    override suspend fun delete(id: String) = incidentRequest {
        val response = api.delete(id)
        if (!response.isSuccessful) throw HttpException(response)
    }
}
