package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentsUseCase
import com.hyejeanmoon.fieldincidentreporter.feature.toIncidentFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    private val getIncidents: GetIncidentsUseCase,
    private val getIncident: GetIncidentUseCase,
) : ViewModel() {
    val incidents = getIncidents().cachedIn(viewModelScope)
    private val _uiState = MutableStateFlow(IncidentListUiState())
    val uiState = _uiState.asStateFlow()

    fun onIncidentSelected(incident: Incident) = reduce(IncidentListStateChange.IncidentSelected(incident))

    fun dismissLookupError() = reduce(IncidentListStateChange.LookupErrorDismissed)

    fun ensureIncident(id: String) {
        if (_uiState.value.selectedIncident?.id == id || _uiState.value.loadingIncidentId == id) return
        reduce(IncidentListStateChange.LookupStarted(id))
        viewModelScope.launch {
            try {
                reduce(IncidentListStateChange.LookupSucceeded(getIncident(id)))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reduce(IncidentListStateChange.LookupFailed(id, error.toIncidentFailure()))
            }
        }
    }

    fun onIncidentSaved(incident: Incident) {
        reduce(IncidentListStateChange.IncidentSaved(incident))
    }

    fun onIncidentDeleted(originEntryId: String, id: String) {
        reduce(IncidentListStateChange.Deleted(id, originEntryId))
    }

    fun acknowledgeDeletion(entryId: String) {
        reduce(IncidentListStateChange.DeleteAcknowledged(entryId))
    }

    private fun reduce(change: IncidentListStateChange) {
        _uiState.update { IncidentListUiStateReducer.reduce(it, change) }
    }
}
