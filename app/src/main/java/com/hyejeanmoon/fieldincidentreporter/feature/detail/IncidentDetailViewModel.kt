package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import com.hyejeanmoon.fieldincidentreporter.domain.error.toIncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentChange
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.DeleteIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ObserveIncidentChangesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getIncident: GetIncidentUseCase,
    private val deleteIncident: DeleteIncidentUseCase,
    observeIncidentChanges: ObserveIncidentChangesUseCase,
) : ViewModel() {
    private val incidentId = savedStateHandle.toIncidentDetailRoute().incidentId
    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
        // An edit saved from the form shows here without another request.
        observeIncidentChanges()
            .filterIsInstance<IncidentChange.Saved>()
            .onEach { change ->
                if (change.incident.id == incidentId) _uiState.update { it.copy(incident = change.incident) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: IncidentDetailAction) {
        when (action) {
            IncidentDetailAction.Retry -> load()
            IncidentDetailAction.Delete -> delete()
            IncidentDetailAction.ErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun load() {
        _uiState.update { it.copy(loadFailure = null) }
        viewModelScope.launch {
            runSuspendCatching { getIncident(incidentId) }
                .onSuccess { incident -> _uiState.update { it.copy(incident = incident) } }
                .onFailure { error -> _uiState.update { it.copy(loadFailure = error.toIncidentFailure()) } }
        }
    }

    private fun delete() {
        val state = _uiState.value
        if (state.incident == null || state.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            runSuspendCatching { deleteIncident(incidentId) }
                // Stay in the deleting state so that nothing can be pressed while the screen closes.
                .onSuccess { _uiState.update { it.copy(isDeleted = true) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isDeleting = false, error = error.toIncidentFailure()) }
                }
        }
    }
}
