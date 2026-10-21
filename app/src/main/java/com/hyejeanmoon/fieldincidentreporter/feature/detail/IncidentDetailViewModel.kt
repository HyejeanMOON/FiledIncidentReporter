package com.hyejeanmoon.fieldincidentreporter.feature.detail

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

sealed interface IncidentDetailAction {
    data class IncidentChanged(val incident: Incident) : IncidentDetailAction
    data object Delete : IncidentDetailAction
    data object ErrorDismissed : IncidentDetailAction
    data class DeleteAcknowledged(val incidentId: String) : IncidentDetailAction
}

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    private val repository: IncidentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: IncidentDetailAction) {
        when (action) {
            is IncidentDetailAction.IncidentChanged -> reduce(IncidentDetailStateChange.IncidentChanged(action.incident))
            IncidentDetailAction.Delete -> deleteIncident()
            IncidentDetailAction.ErrorDismissed -> reduce(IncidentDetailStateChange.ErrorCleared)
            is IncidentDetailAction.DeleteAcknowledged -> reduce(
                IncidentDetailStateChange.DeleteAcknowledged(action.incidentId),
            )
        }
    }

    private fun deleteIncident() {
        val incident = _uiState.value.incident ?: return
        if (_uiState.value.isDeleting || _uiState.value.deletedIncidentId != null) return
        reduce(IncidentDetailStateChange.DeleteStarted)
        viewModelScope.launch {
            try {
                repository.delete(incident.id)
                reduce(IncidentDetailStateChange.DeleteSucceeded(incident.id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reduce(IncidentDetailStateChange.DeleteFailed(error.toIncidentFailure()))
            } finally {
                reduce(IncidentDetailStateChange.DeleteFinished)
            }
        }
    }

    private fun reduce(change: IncidentDetailStateChange) {
        _uiState.update { IncidentDetailUiStateReducer.reduce(it, change) }
    }
}
