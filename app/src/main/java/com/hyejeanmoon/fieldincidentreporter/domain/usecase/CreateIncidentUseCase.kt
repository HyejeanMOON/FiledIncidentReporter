package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import javax.inject.Inject

class CreateIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository,
) {
    suspend operator fun invoke(
        draft: IncidentDraft,
    ): Incident {
        val normalized = draft.normalized()
        return repository.create(
            normalized.title,
            normalized.location,
            normalized.description,
            normalized.photos,
        )
    }
}
