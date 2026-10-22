package com.hyejeanmoon.fieldincidentreporter.data.repository

import android.content.Context
import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.RetrofitIncidentPhotoUrlResolver
import com.hyejeanmoon.fieldincidentreporter.data.remote.incidentRequest
import com.hyejeanmoon.fieldincidentreporter.data.remote.photoPart
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncident
import com.hyejeanmoon.fieldincidentreporter.data.remote.toSummary
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentUpdateBody
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPage
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import javax.inject.Inject

internal class RetrofitIncidentRepository @Inject constructor(
    private val api: IncidentApi,
    private val photoUrlResolver: RetrofitIncidentPhotoUrlResolver,
    @ApplicationContext private val context: Context,
) : IncidentRepository {
    override suspend fun getIncidentsPage(page: Int, size: Int): IncidentPage = incidentRequest {
        val response = api.getIncidents(page, size)
        IncidentPage(response.items.map { it.toSummary() }, response.page, response.hasNext)
    }

    override suspend fun getIncident(id: String): Incident = incidentRequest {
        api.getIncident(id).toIncident(photoUrlResolver)
    }

    override suspend fun create(
        title: String,
        location: String,
        description: String,
        photos: List<IncidentPhoto>,
    ): Incident = incidentRequest {
        api.create(
            IncidentBody(title, location, description),
            photos.mapIndexed { index, photo ->
                require(photo is IncidentPhoto.Local) { "New incidents require local photos" }
                photoPart(context, photo.uri, index)
            },
        ).toIncident(photoUrlResolver)
    }

    override suspend fun update(incident: Incident) = incidentRequest {
        val localPhotos = incident.photos.filterIsInstance<IncidentPhoto.Local>()
        val retainedPhotos = incident.photos.filterIsInstance<IncidentPhoto.Stored>()
        api.update(
            incident.id,
            IncidentUpdateBody(
                incident.id,
                incident.title,
                incident.location,
                incident.description,
                retainedPhotos.map(IncidentPhoto.Stored::reference),
            ),
            localPhotos.mapIndexed { index, photo -> photoPart(context, photo.uri, index) },
        ).toIncident(photoUrlResolver)
    }

    override suspend fun delete(id: String) = incidentRequest {
        val response = api.delete(id)
        if (!response.isSuccessful) throw HttpException(response)
    }
}
