package com.hyejeanmoon.fieldincidentreporter.feature.list

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

data class IncidentListUiState(
    val incidents: List<Incident> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: IncidentFailure? = null,
)
