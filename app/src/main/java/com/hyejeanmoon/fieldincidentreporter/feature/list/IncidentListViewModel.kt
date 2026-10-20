package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.feature.toIncidentFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(private val repository: IncidentRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(IncidentListUiState())
    val uiState = _uiState.asStateFlow()
    private val eventChannel = Channel<IncidentListEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var localRevision = 0L

    init { refresh() }

    fun refresh() {
        if (_uiState.value.isLoading) return
        val startingRevision = localRevision
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            var needsRefresh = false
            try {
                val incidents = repository.getIncidents()
                if (startingRevision == localRevision) {
                    _uiState.value = _uiState.value.copy(incidents = incidents)
                } else {
                    needsRefresh = true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(error = error.toIncidentFailure())
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (needsRefresh) refresh()
            }
        }
    }

    fun onIncidentSaved(incident: Incident) {
        localRevision++
        val incidents = _uiState.value.incidents
        _uiState.value = _uiState.value.copy(
            incidents = if (incidents.any { it.id == incident.id }) {
                incidents.map { if (it.id == incident.id) incident else it }
            } else {
                listOf(incident) + incidents
            },
        )
    }

    fun removeIncident(originEntryId: String, id: String) {
        if (_uiState.value.isDeleting) return
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            try {
                repository.delete(id)
                localRevision++
                _uiState.value = _uiState.value.copy(
                    incidents = _uiState.value.incidents.filterNot { it.id == id },
                    error = null,
                )
                eventChannel.send(IncidentListEvent(originEntryId))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(error = error.toIncidentFailure())
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
