package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import javax.inject.Inject

/** The outcome of loading or deleting, fed back into [IncidentDetailReducer]. */
sealed interface IncidentDetailResult {
    data class Loaded(val incident: Incident) : IncidentDetailResult
    data class LoadFailed(val failure: IncidentFailure) : IncidentDetailResult
    data object Deleted : IncidentDetailResult
    data class DeleteFailed(val failure: IncidentFailure) : IncidentDetailResult
}

/**
 * Computes each detail state from the previous one. It only marks loading or deleting as started;
 * [IncidentDetailViewModel] does that work and reports back with an [IncidentDetailResult].
 */
class IncidentDetailReducer @Inject constructor() {
    fun reduce(state: IncidentDetailUiState, action: IncidentDetailAction): IncidentDetailUiState = when (action) {
        IncidentDetailAction.Retry ->
            if (state.loadFailure != null) state.copy(isLoading = true, loadFailure = null) else state
        IncidentDetailAction.Delete ->
            if (state.incident != null && !state.isDeleting) state.copy(isDeleting = true, error = null) else state
        IncidentDetailAction.ErrorDismissed -> state.copy(error = null)
    }

    fun reduce(state: IncidentDetailUiState, result: IncidentDetailResult): IncidentDetailUiState = when (result) {
        is IncidentDetailResult.Loaded -> state.copy(isLoading = false, incident = result.incident)
        is IncidentDetailResult.LoadFailed -> state.copy(isLoading = false, loadFailure = result.failure)
        // Stay in the deleting state so that nothing can be pressed while the screen closes.
        IncidentDetailResult.Deleted -> state.copy(isDeleted = true)
        is IncidentDetailResult.DeleteFailed -> state.copy(isDeleting = false, error = result.failure)
    }
}
