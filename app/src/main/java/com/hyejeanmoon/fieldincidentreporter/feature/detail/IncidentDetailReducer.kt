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
            if (state is IncidentDetailUiState.LoadFailed) IncidentDetailUiState.Loading else state
        IncidentDetailAction.Reload -> state.ifLoaded { if (isBusy) this else copy(isReloading = true, error = null) }
        IncidentDetailAction.Delete -> state.ifLoaded { if (isBusy) this else copy(isDeleting = true, error = null) }
        IncidentDetailAction.ErrorDismissed -> state.ifLoaded { copy(error = null) }
    }

    fun reduce(state: IncidentDetailUiState, result: IncidentDetailResult): IncidentDetailUiState = when (result) {
        is IncidentDetailResult.Loaded -> IncidentDetailUiState.Loaded(result.incident)
        // A failed reload keeps the copy already shown and reports the failure over it.
        is IncidentDetailResult.LoadFailed -> if (state is IncidentDetailUiState.Loaded) {
            state.copy(isReloading = false, error = result.failure)
        } else {
            IncidentDetailUiState.LoadFailed(result.failure)
        }
        IncidentDetailResult.Deleted -> state.ifLoaded { copy(isDeleted = true) }
        is IncidentDetailResult.DeleteFailed -> state.ifLoaded { copy(isDeleting = false, error = result.failure) }
    }
}

private val IncidentDetailUiState.Loaded.isBusy get() = isReloading || isDeleting

private inline fun IncidentDetailUiState.ifLoaded(
    transform: IncidentDetailUiState.Loaded.() -> IncidentDetailUiState,
) = if (this is IncidentDetailUiState.Loaded) transform() else this
