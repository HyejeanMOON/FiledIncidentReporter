package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

internal sealed interface IncidentDetailStateChange {
    data class LoadStarted(val id: String) : IncidentDetailStateChange
    data class LoadSucceeded(val id: String, val incident: Incident) : IncidentDetailStateChange
    data class LoadFailed(val id: String, val failure: IncidentFailure) : IncidentDetailStateChange
    data object DeleteStarted : IncidentDetailStateChange
    data class DeleteSucceeded(val incidentId: String) : IncidentDetailStateChange
    data class DeleteFailed(val failure: IncidentFailure) : IncidentDetailStateChange
    data object DeleteFinished : IncidentDetailStateChange
    data object ErrorCleared : IncidentDetailStateChange
    data class DeleteAcknowledged(val incidentId: String) : IncidentDetailStateChange
}

internal object IncidentDetailUiStateReducer {
    fun reduce(
        previous: IncidentDetailUiState,
        change: IncidentDetailStateChange,
    ): IncidentDetailUiState = when (change) {
        is IncidentDetailStateChange.LoadStarted -> previous.copy(
            incident = previous.incident?.takeIf { it.id == change.id },
            loadingIncidentId = change.id,
            loadFailure = null,
            error = null,
        )
        is IncidentDetailStateChange.LoadSucceeded -> if (previous.loadingIncidentId == change.id) {
            previous.copy(
                incident = change.incident,
                loadingIncidentId = null,
                loadFailure = null,
                error = null,
            )
        } else previous
        is IncidentDetailStateChange.LoadFailed -> if (previous.loadingIncidentId == change.id) {
            previous.copy(
                loadingIncidentId = null,
                loadFailure = change.failure,
                error = change.failure.takeIf { previous.incident != null },
            )
        } else previous
        IncidentDetailStateChange.DeleteStarted -> previous.copy(isDeleting = true, error = null)
        is IncidentDetailStateChange.DeleteSucceeded -> previous.copy(
            error = null,
            deletedIncidentId = change.incidentId,
        )
        is IncidentDetailStateChange.DeleteFailed -> previous.copy(error = change.failure)
        IncidentDetailStateChange.DeleteFinished -> previous.copy(isDeleting = false)
        IncidentDetailStateChange.ErrorCleared -> previous.copy(error = null)
        is IncidentDetailStateChange.DeleteAcknowledged -> if (previous.deletedIncidentId == change.incidentId) {
            previous.copy(deletedIncidentId = null)
        } else previous
    }
}
