package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.feature.toIncidentFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(private val repository: IncidentRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(IncidentListUiState())
    val uiState = _uiState.asStateFlow()
    private var localRevision = 0L

    init { refresh() }

    fun refresh() {
        if (_uiState.value.isLoading) return
        val startingRevision = localRevision
        reduce(IncidentListStateChange.RefreshStarted)
        viewModelScope.launch {
            var needsRefresh = false
            try {
                val incidents = repository.getIncidents()
                if (startingRevision == localRevision) {
                    reduce(IncidentListStateChange.IncidentsLoaded(incidents))
                } else {
                    needsRefresh = true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reduce(IncidentListStateChange.RefreshFailed(error.toIncidentFailure()))
            } finally {
                reduce(IncidentListStateChange.RefreshFinished)
                if (needsRefresh) refresh()
            }
        }
    }

    fun onIncidentSaved(incident: Incident) {
        localRevision++
        reduce(IncidentListStateChange.IncidentSaved(incident))
    }

    fun onIncidentDeleted(originEntryId: String, id: String) {
        localRevision++
        reduce(IncidentListStateChange.Deleted(id, originEntryId))
    }

    fun clearError() {
        reduce(IncidentListStateChange.ErrorCleared)
    }

    fun acknowledgeDeletion(entryId: String) {
        reduce(IncidentListStateChange.DeleteAcknowledged(entryId))
    }

    private fun reduce(change: IncidentListStateChange) {
        _uiState.update { IncidentListUiStateReducer.reduce(it, change) }
    }
}
