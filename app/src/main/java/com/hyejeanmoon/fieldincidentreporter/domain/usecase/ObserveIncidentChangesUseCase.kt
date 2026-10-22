package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import javax.inject.Inject

class ObserveIncidentChangesUseCase @Inject constructor(
    private val repository: IncidentRepository,
) {
    operator fun invoke() = repository.changes
}
