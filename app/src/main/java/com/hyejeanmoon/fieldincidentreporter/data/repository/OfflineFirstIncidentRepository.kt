package com.hyejeanmoon.fieldincidentreporter.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentDao
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentEntity
import com.hyejeanmoon.fieldincidentreporter.data.local.toEntity
import com.hyejeanmoon.fieldincidentreporter.data.local.toIncident
import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.incidentRequest
import com.hyejeanmoon.fieldincidentreporter.data.remote.photoPart
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncident
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentException
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject

/** Room is the source of truth for the list; the server refreshes it, and confirmed writes update it. */
internal class OfflineFirstIncidentRepository @Inject constructor(
    private val api: IncidentApi,
    retrofit: Retrofit,
    @ApplicationContext private val context: Context,
    private val dao: IncidentDao,
) : IncidentRepository {
    private val baseUrl = retrofit.baseUrl()

    @OptIn(ExperimentalPagingApi::class)
    override fun incidents(): Flow<PagingData<Incident>> = Pager(
        config = PagingConfig(pageSize = INCIDENT_PAGE_SIZE, prefetchDistance = 5, enablePlaceholders = false),
        remoteMediator = IncidentRemoteMediator(api, dao, baseUrl),
        pagingSourceFactory = dao::pagingSource,
    ).flow.map { data -> data.map(IncidentEntity::toIncident) }

    /** A lookup also refreshes the cached copy, and a report the server no longer has leaves the cache. */
    override suspend fun getIncident(id: String): Incident {
        val incident = try {
            incidentRequest { api.getIncident(id).toIncident(baseUrl) }
        } catch (error: IncidentException) {
            if (error.failure == IncidentFailure.INCIDENT_NOT_FOUND) updateCache("lookup") { dao.delete(listOf(id)) }
            throw error
        }
        updateCache("lookup") { dao.refreshCached(incident.toEntity()) }
        return incident
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
        updateCache("delete") { dao.delete(listOf(id)) }
    }

    private suspend fun save(request: suspend () -> IncidentDto): Incident =
        incidentRequest { request().toIncident(baseUrl) }
            .also { incident -> updateCache("save") { dao.upsert(listOf(incident.toEntity())) } }

    /** The server write has succeeded, so a cache failure must not fail it; the next refresh repairs the cache. */
    private suspend fun updateCache(write: String, block: suspend () -> Unit) {
        runSuspendCatching { block() }
            .onFailure { Log.w("IncidentRepository", "Could not update cache after server $write", it) }
    }

    private fun IncidentDraft.toBody(deletePhotoIds: List<String>? = null) =
        IncidentBody(title, location, description, deletePhotoIds)

    /** Only photos picked on the device are uploaded; stored photos are kept by the server. */
    private fun IncidentDraft.newPhotoParts(): List<MultipartBody.Part> =
        photos.filterIsInstance<IncidentPhoto.Local>()
            .mapIndexed { index, photo -> photoPart(context, photo.uri, index) }
}
