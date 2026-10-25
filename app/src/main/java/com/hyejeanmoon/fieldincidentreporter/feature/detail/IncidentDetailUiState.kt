package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

sealed interface IncidentDetailUiState {
    data object Loading : IncidentDetailUiState

    /** The report could not be loaded at all. */
    data class LoadFailed(val failure: IncidentFailure) : IncidentDetailUiState

    data class Loaded(
        val incident: Incident,
        /** Fetching the report again after it was edited, while the old copy stays on screen. */
        val isReloading: Boolean = false,
        val isDeleting: Boolean = false,
        /** Set once the report is gone; [isDeleting] stays set so nothing can be pressed while the screen closes. */
        val isDeleted: Boolean = false,
        /** Shown as a dialog over the report. */
        val error: IncidentFailure? = null,
    ) : IncidentDetailUiState
}
