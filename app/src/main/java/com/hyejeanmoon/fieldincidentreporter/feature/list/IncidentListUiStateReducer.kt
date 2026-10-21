package com.hyejeanmoon.fieldincidentreporter.feature.list

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

internal sealed interface IncidentListStateChange {
    data class LookupStarted(val id: String) : IncidentListStateChange
    data class LookupSucceeded(val incident: Incident) : IncidentListStateChange
    data class LookupFailed(val id: String, val failure: IncidentFailure) : IncidentListStateChange
    data object LookupErrorDismissed : IncidentListStateChange
    data class IncidentSelected(val incident: Incident) : IncidentListStateChange
    data class IncidentSaved(val incident: Incident) : IncidentListStateChange
    data class Deleted(val incidentId: String, val originEntryId: String) : IncidentListStateChange
    data class DeleteAcknowledged(val entryId: String) : IncidentListStateChange
}

internal object IncidentListUiStateReducer {
    fun reduce(previous: IncidentListUiState, change: IncidentListStateChange): IncidentListUiState = when (change) {
        is IncidentListStateChange.LookupStarted -> previous.copy(
            selectedIncident = previous.selectedIncident?.takeIf { it.id == change.id },
            loadingIncidentId = change.id,
            failedIncidentId = null,
            lookupFailure = null,
            lookupDialogVisible = false,
        )
        is IncidentListStateChange.LookupSucceeded -> if (previous.loadingIncidentId == change.incident.id) {
            previous.copy(selectedIncident = change.incident, loadingIncidentId = null, failedIncidentId = null, lookupFailure = null, lookupDialogVisible = false)
        } else previous
        is IncidentListStateChange.LookupFailed -> if (previous.loadingIncidentId == change.id) {
            previous.copy(loadingIncidentId = null, failedIncidentId = change.id, lookupFailure = change.failure, lookupDialogVisible = true)
        } else previous
        IncidentListStateChange.LookupErrorDismissed -> previous.copy(lookupDialogVisible = false)
        is IncidentListStateChange.IncidentSelected -> previous.copy(
            selectedIncident = change.incident,
            loadingIncidentId = null,
            failedIncidentId = null,
            lookupFailure = null,
            lookupDialogVisible = false,
        )
        is IncidentListStateChange.IncidentSaved -> previous.copy(
            selectedIncident = if (previous.selectedIncident?.id == change.incident.id) change.incident else previous.selectedIncident,
        )
        is IncidentListStateChange.Deleted -> previous.copy(
            selectedIncident = previous.selectedIncident?.takeUnless { it.id == change.incidentId },
            failedIncidentId = null,
            lookupFailure = null,
            lookupDialogVisible = false,
            deletedFromEntryId = change.originEntryId,
        )
        is IncidentListStateChange.DeleteAcknowledged -> if (previous.deletedFromEntryId == change.entryId) {
            previous.copy(deletedFromEntryId = null)
        } else previous
    }
}
