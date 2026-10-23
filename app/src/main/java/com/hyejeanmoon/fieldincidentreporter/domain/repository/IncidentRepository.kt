package com.hyejeanmoon.fieldincidentreporter.domain.repository

import androidx.paging.PagingData
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    /** Reports newest first, read from the local cache, which loads pages from the server as the list grows. */
    fun incidents(): Flow<PagingData<Incident>>
    suspend fun getIncident(id: String): Incident
    suspend fun create(draft: IncidentDraft): Incident
    suspend fun update(id: String, draft: IncidentDraft, deletePhotoIds: List<String>): Incident
    suspend fun delete(id: String)
}
