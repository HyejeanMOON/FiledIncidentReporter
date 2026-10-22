package com.hyejeanmoon.fieldincidentreporter.domain.repository

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPage
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto

interface IncidentRepository {
    suspend fun getIncidentsPage(page: Int, size: Int): IncidentPage
    suspend fun getIncident(id: String): Incident
    suspend fun create(title: String, location: String, description: String, photos: List<IncidentPhoto>): Incident
    suspend fun update(id: String, draft: IncidentDraft, deletePhotoIds: List<String>): Incident
    suspend fun delete(id: String)
}
