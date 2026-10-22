package com.hyejeanmoon.fieldincidentreporter.testing

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentChange
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPage
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant

val testTime: Instant = Instant.parse("2026-10-20T09:00:00Z")

fun testIncident(id: String = "incident-1", title: String = "Title") =
    Incident(id, title, "Room", "Details", createdAt = testTime, updatedAt = testTime)

class FakeIncidentRepository(private val incidents: MutableMap<String, Incident> = mutableMapOf()) : IncidentRepository {
    override val changes = MutableSharedFlow<IncidentChange>(extraBufferCapacity = 16)
    var failure: Throwable? = null
    val updates = mutableListOf<Triple<String, IncidentDraft, List<String>>>()

    override suspend fun getIncidentsPage(page: Int, size: Int) =
        IncidentPage(incidents.values.toList(), page, hasNext = false)

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
        changes.emit(IncidentChange.Deleted(id))
    }

    private suspend fun save(id: String, draft: IncidentDraft): Incident {
        failure?.let { throw it }
        val incident = Incident(id, draft.title, draft.location, draft.description, draft.photos, testTime, testTime)
        incidents[id] = incident
        changes.emit(IncidentChange.Saved(incident))
        return incident
    }
}
