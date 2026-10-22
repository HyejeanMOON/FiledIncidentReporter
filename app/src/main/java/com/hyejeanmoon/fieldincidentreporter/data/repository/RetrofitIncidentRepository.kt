package com.hyejeanmoon.fieldincidentreporter.data.repository

import android.content.Context
import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.incidentRequest
import com.hyejeanmoon.fieldincidentreporter.data.remote.photoPart
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncident
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncidentPage
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentChange
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPage
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.MultipartBody
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject

internal class RetrofitIncidentRepository @Inject constructor(
    private val api: IncidentApi,
    retrofit: Retrofit,
    @ApplicationContext private val context: Context,
) : IncidentRepository {
    private val baseUrl = retrofit.baseUrl()
    private val _changes = MutableSharedFlow<IncidentChange>(extraBufferCapacity = 16)
    override val changes = _changes.asSharedFlow()

    override suspend fun getIncidentsPage(page: Int, size: Int): IncidentPage = incidentRequest {
        api.getIncidents(page, size).toIncidentPage(baseUrl)
    }

    override suspend fun getIncident(id: String): Incident = incidentRequest {
        api.getIncident(id).toIncident(baseUrl)
    }

    override suspend fun create(draft: IncidentDraft): Incident = save {
        api.create(draft.toBody(), draft.newPhotoParts())
    }

    override suspend fun update(id: String, draft: IncidentDraft, deletePhotoIds: List<String>): Incident = save {
        api.update(id, draft.toBody(deletePhotoIds), draft.newPhotoParts())
    }

    override suspend fun delete(id: String) {
        incidentRequest {
            val response = api.delete(id)
            if (!response.isSuccessful) throw HttpException(response)
        }
        _changes.emit(IncidentChange.Deleted(id))
    }

    private suspend fun save(request: suspend () -> IncidentDto): Incident =
        incidentRequest { request().toIncident(baseUrl) }
            .also { _changes.emit(IncidentChange.Saved(it)) }

    private fun IncidentDraft.toBody(deletePhotoIds: List<String>? = null) =
        IncidentBody(title, location, description, deletePhotoIds)

    /** Only photos picked on the device are uploaded; stored photos are kept by the server. */
    private fun IncidentDraft.newPhotoParts(): List<MultipartBody.Part> =
        photos.filterIsInstance<IncidentPhoto.Local>()
            .mapIndexed { index, photo -> photoPart(context, photo.uri, index) }
}
