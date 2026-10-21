package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.feature.toIncidentFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INITIALIZED_KEY = "form_initialized"
private const val INCIDENT_ID_KEY = "form_incident_id"
private const val TITLE_KEY = "form_title"
private const val LOCATION_KEY = "form_location"
private const val DESCRIPTION_KEY = "form_description"
private const val PHOTOS_KEY = "form_photos"
private const val VALIDATED_FIELDS_KEY = "form_validated_fields"

@HiltViewModel
class IncidentFormViewModel @Inject constructor(
    private val repository: IncidentRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(restoreFormState(savedStateHandle))
    val uiState = _uiState.asStateFlow()

    fun initialize(incident: Incident?) {
        reduce(IncidentFormStateChange.Initialized(incident), persistForm = true)
    }

    fun onEvent(event: IncidentFormUiEvent) {
        when (event) {
            is IncidentFormUiEvent.TitleChanged -> reduce(
                IncidentFormStateChange.TitleChanged(event.value), persistForm = true,
            )
            is IncidentFormUiEvent.LocationChanged -> reduce(
                IncidentFormStateChange.LocationChanged(event.value), persistForm = true,
            )
            is IncidentFormUiEvent.DescriptionChanged -> reduce(
                IncidentFormStateChange.DescriptionChanged(event.value), persistForm = true,
            )
            is IncidentFormUiEvent.PhotosSelected -> reduce(
                IncidentFormStateChange.PhotosSelected(event.uris), persistForm = true,
            )
            IncidentFormUiEvent.PhotosRemoved -> reduce(IncidentFormStateChange.PhotosRemoved, persistForm = true)
            is IncidentFormUiEvent.FieldFocusChanged -> {
                val wasFocused = event.field in _uiState.value.focusedFields
                if (event.isFocused || wasFocused) {
                    reduce(
                        IncidentFormStateChange.FieldFocusChanged(event.field, event.isFocused),
                        persistForm = !event.isFocused,
                    )
                }
            }
            IncidentFormUiEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (!current.isSubmitEnabled) return
        reduce(IncidentFormStateChange.SaveStarted)
        val request = _uiState.value
        if (!request.isSaving) return
        viewModelScope.launch {
            try {
                val saved = if (request.incidentId == null) {
                    repository.create(
                        request.title.trim(),
                        request.location.trim(),
                        request.description.trim(),
                        request.photoUris,
                    )
                } else {
                    repository.update(
                        Incident(
                            request.incidentId,
                            request.title.trim(),
                            request.location.trim(),
                            request.description.trim(),
                            request.photoUris,
                        ),
                    )
                }
                reduce(IncidentFormStateChange.SaveSucceeded(saved))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reduce(IncidentFormStateChange.SaveFailed(error.toIncidentFailure()))
            } finally {
                reduce(IncidentFormStateChange.SaveFinished)
            }
        }
    }

    fun clearError() {
        reduce(IncidentFormStateChange.ErrorCleared)
    }

    fun acknowledgeSaved(incident: Incident) {
        reduce(IncidentFormStateChange.SavedAcknowledged(incident))
    }

    private fun reduce(change: IncidentFormStateChange, persistForm: Boolean = false) {
        _uiState.update { IncidentFormUiStateReducer.reduce(it, change) }
        if (persistForm) persist(_uiState.value)
    }

    private fun persist(state: IncidentFormUiState) {
        savedStateHandle[INITIALIZED_KEY] = state.isInitialized
        savedStateHandle[INCIDENT_ID_KEY] = state.incidentId
        savedStateHandle[TITLE_KEY] = state.title
        savedStateHandle[LOCATION_KEY] = state.location
        savedStateHandle[DESCRIPTION_KEY] = state.description
        savedStateHandle[PHOTOS_KEY] = ArrayList(state.photoUris)
        savedStateHandle[VALIDATED_FIELDS_KEY] = ArrayList(state.validatedFields.map { it.name })
    }
}

private fun restoreFormState(handle: SavedStateHandle): IncidentFormUiState = IncidentFormUiState(
    isInitialized = handle[INITIALIZED_KEY] ?: false,
    incidentId = handle[INCIDENT_ID_KEY],
    title = handle[TITLE_KEY] ?: "",
    location = handle[LOCATION_KEY] ?: "",
    description = handle[DESCRIPTION_KEY] ?: "",
    photoUris = handle.get<ArrayList<String>>(PHOTOS_KEY)?.toList().orEmpty(),
    validatedFields = handle.get<ArrayList<String>>(VALIDATED_FIELDS_KEY)
        ?.mapNotNull { value -> RequiredIncidentField.entries.firstOrNull { it.name == value } }
        ?.toSet()
        .orEmpty(),
).let { IncidentFormUiStateReducer.reduce(it, IncidentFormStateChange.Restored) }
