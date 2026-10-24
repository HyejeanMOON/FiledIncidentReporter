package com.hyejeanmoon.fieldincidentreporter.testing

import androidx.paging.PagingData
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

val testTime: Instant = Instant.parse("2026-10-20T09:00:00Z")

fun testIncident(id: String = "incident-1", title: String = "Title") =
    Incident(id, title, "Room", "Details", createdAt = testTime, updatedAt = testTime)

class FakeIncidentRepository(val incidents: MutableMap<String, Incident> = mutableMapOf()) : IncidentRepository {
    var failure: Throwable? = null
    val updates = mutableListOf<Triple<String, IncidentDraft, List<String>>>()

    override fun incidents(): Flow<PagingData<Incident>> = flowOf(PagingData.from(incidents.values.toList()))

    override suspend fun getIncident(id: String): Incident {
        failure?.let { throw it }
        return incidents.getValue(id)
    }

    override suspend fun create(draft: IncidentDraft): Incident = save("new", draft)

    override suspend fun update(id: String, draft: IncidentDraft, deletePhotoIds: List<String>): Incident {
        updates += Triple(id, draft, deletePhotoIds)
        return save(id, draft)
    }

    override suspend fun delete(id: String) {
        failure?.let { throw it }
        incidents.remove(id)
    }

    private suspend fun save(id: String, draft: IncidentDraft): Incident {
        failure?.let { throw it }
        val incident = Incident(id, draft.title, draft.location, draft.description, draft.photos, testTime, testTime)
        incidents[id] = incident
        return incident
    }
}
