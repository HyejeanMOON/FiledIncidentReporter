package com.hyejeanmoon.fieldincidentreporter.domain.repository

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentChange
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPage
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    val changes: Flow<IncidentChange>
    suspend fun getIncidentsPage(page: Int, size: Int): IncidentPage
    suspend fun getIncident(id: String): Incident
    suspend fun create(draft: IncidentDraft): Incident
    suspend fun update(id: String, draft: IncidentDraft, deletePhotoIds: List<String>): Incident
    suspend fun delete(id: String)
}
