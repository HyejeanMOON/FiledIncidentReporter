package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

data class IncidentDetailUiState(
    val incident: Incident? = null,
    /** Set when the report could not be loaded at all. */
    val loadFailure: IncidentFailure? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    /** Shown as a dialog over the loaded report. */
    val error: IncidentFailure? = null,
)

sealed interface IncidentDetailAction {
    data object Retry : IncidentDetailAction
    data object Delete : IncidentDetailAction
    data object ErrorDismissed : IncidentDetailAction
}
