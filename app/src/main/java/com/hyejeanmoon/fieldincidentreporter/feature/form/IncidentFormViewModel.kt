package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.feature.toIncidentFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class IncidentFormViewModel(private val repository: IncidentRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(IncidentFormUiState())
    val uiState = _uiState.asStateFlow()
    private val eventChannel = Channel<IncidentFormEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun saveIncident(
        originEntryId: String,
        id: String?,
        title: String,
        location: String,
        description: String,
        photoUris: List<String>,
    ) {
        if (_uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val saved = if (id == null) {
                    repository.create(title, location, description, photoUris)
                } else {
                    repository.update(Incident(id, title, location, description, photoUris))
                }
                eventChannel.send(IncidentFormEvent(originEntryId, saved))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(error = error.toIncidentFailure())
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        fun factory(repository: IncidentRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(IncidentFormViewModel::class.java))
                return IncidentFormViewModel(repository) as T
            }
        }
    }
}
