package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import javax.inject.Inject

class UpdateIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository,
) {
    suspend operator fun invoke(incident: Incident): Incident {
        val normalized = IncidentDraft(
            title = incident.title,
            location = incident.location,
            description = incident.description,
            photos = incident.photos,
        ).normalized()
        return repository.update(
            incident.copy(
                title = normalized.title,
                location = normalized.location,
                description = normalized.description,
                photos = normalized.photos,
            ),
        )
    }
}
