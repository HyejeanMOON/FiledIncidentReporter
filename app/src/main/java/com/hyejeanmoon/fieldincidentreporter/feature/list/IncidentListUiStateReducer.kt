package com.hyejeanmoon.fieldincidentreporter.feature.list

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

internal sealed interface IncidentListStateChange {
    data object RefreshStarted : IncidentListStateChange
    data class IncidentsLoaded(val incidents: List<Incident>) : IncidentListStateChange
    data class RefreshFailed(val failure: IncidentFailure) : IncidentListStateChange
    data object RefreshFinished : IncidentListStateChange
    data class IncidentSaved(val incident: Incident) : IncidentListStateChange
    data class Deleted(val incidentId: String, val originEntryId: String) : IncidentListStateChange
    data object ErrorCleared : IncidentListStateChange
    data class DeleteAcknowledged(val entryId: String) : IncidentListStateChange
}

internal object IncidentListUiStateReducer {
    fun reduce(previous: IncidentListUiState, change: IncidentListStateChange): IncidentListUiState = when (change) {
        IncidentListStateChange.RefreshStarted -> previous.copy(isLoading = true, error = null)
        is IncidentListStateChange.IncidentsLoaded -> previous.copy(incidents = change.incidents)
        is IncidentListStateChange.RefreshFailed -> previous.copy(error = change.failure)
        IncidentListStateChange.RefreshFinished -> previous.copy(isLoading = false)
        is IncidentListStateChange.IncidentSaved -> previous.copy(
            incidents = if (previous.incidents.any { it.id == change.incident.id }) {
                previous.incidents.map { if (it.id == change.incident.id) change.incident else it }
            } else {
                listOf(change.incident) + previous.incidents
            },
            error = null,
        )
        is IncidentListStateChange.Deleted -> previous.copy(
            incidents = previous.incidents.filterNot { it.id == change.incidentId },
            error = null,
            deletedFromEntryId = change.originEntryId,
        )
        IncidentListStateChange.ErrorCleared -> previous.copy(error = null)
        is IncidentListStateChange.DeleteAcknowledged -> if (previous.deletedFromEntryId == change.entryId) {
            previous.copy(deletedFromEntryId = null)
        } else previous
    }
}
