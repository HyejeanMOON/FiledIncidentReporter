package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import com.hyejeanmoon.fieldincidentreporter.domain.error.toIncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.feature.detail.state.IncidentDetailReducer
import com.hyejeanmoon.fieldincidentreporter.feature.detail.state.IncidentDetailResult
import com.hyejeanmoon.fieldincidentreporter.feature.detail.state.IncidentDetailUiEvent
import com.hyejeanmoon.fieldincidentreporter.feature.detail.state.IncidentDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IncidentRepository,
    private val reducer: IncidentDetailReducer,
) : ViewModel() {
    private val incidentId = savedStateHandle.toIncidentDetailRoute().incidentId
    private val _uiState = MutableStateFlow<IncidentDetailUiState>(IncidentDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: IncidentDetailUiEvent) {
        val previous = _uiState.value
        val state = reducer.reduce(previous, event)
        _uiState.value = state
        // The reducer only marks the work as started, and leaves the state alone when it should not start.
        if (state != previous) when (event) {
            IncidentDetailUiEvent.Retry, IncidentDetailUiEvent.Reload -> load()
            IncidentDetailUiEvent.Delete -> delete()
            IncidentDetailUiEvent.ErrorDismissed -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            runSuspendCatching { repository.getIncident(incidentId) }
                .onSuccess { onResult(IncidentDetailResult.Loaded(it)) }
                .onFailure { onResult(IncidentDetailResult.LoadFailed(it.toIncidentFailure())) }
        }
    }

    private fun delete() {
        viewModelScope.launch {
            runSuspendCatching { repository.delete(incidentId) }
                .onSuccess { onResult(IncidentDetailResult.Deleted) }
                .onFailure { onResult(IncidentDetailResult.DeleteFailed(it.toIncidentFailure())) }
        }
    }

    private fun onResult(result: IncidentDetailResult) {
        _uiState.value = reducer.reduce(_uiState.value, result)
    }
}
