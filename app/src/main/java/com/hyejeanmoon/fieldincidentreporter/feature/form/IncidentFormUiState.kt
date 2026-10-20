package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure

data class IncidentFormUiState(
    val isSaving: Boolean = false,
    val error: IncidentFailure? = null,
)
