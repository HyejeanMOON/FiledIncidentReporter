package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val loadingIncidentId: String? = null,
    val loadFailure: IncidentFailure? = null,
    val isDeleting: Boolean = false,
    val error: IncidentFailure? = null,
    val deletedIncidentId: String? = null,
)
