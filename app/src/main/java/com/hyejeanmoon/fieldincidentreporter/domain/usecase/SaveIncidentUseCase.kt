package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import javax.inject.Inject

/** Creates a report when [id] is null, otherwise updates it and removes [deletePhotoIds]. */
class SaveIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository,
) {
    suspend operator fun invoke(id: String?, draft: IncidentDraft, deletePhotoIds: List<String>): Incident {
        val normalized = draft.normalized()
        return if (id == null) {
            repository.create(normalized)
        } else {
            repository.update(id, normalized, deletePhotoIds)
        }
    }
}
