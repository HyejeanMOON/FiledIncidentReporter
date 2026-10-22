package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import javax.inject.Inject

class UpdateIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository,
) {
    suspend operator fun invoke(id: String, draft: IncidentDraft, deletePhotoIds: List<String>): Incident =
        repository.update(id, draft.normalized(), deletePhotoIds)
}
