package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import javax.inject.Inject

class GetIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository,
) {
    suspend operator fun invoke(id: String) = repository.getIncident(id)
}
