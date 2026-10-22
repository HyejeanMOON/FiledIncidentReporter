package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.CreateIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.UpdateIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ValidateIncidentDraftUseCase
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
private const val PHOTO_TYPES_KEY = "form_photo_types"
private const val PHOTO_REFERENCES_KEY = "form_photo_references"
private const val PHOTO_DISPLAY_URIS_KEY = "form_photo_display_uris"
private const val ORIGINAL_PHOTO_IDS_KEY = "form_original_photo_ids"
private const val VALIDATED_FIELDS_KEY = "form_validated_fields"

@HiltViewModel
class IncidentFormViewModel @Inject constructor(
    private val createIncident: CreateIncidentUseCase,
    private val getIncident: GetIncidentUseCase,
    private val updateIncident: UpdateIncidentUseCase,
    private val validateIncidentDraft: ValidateIncidentDraftUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        restoreFormState(savedStateHandle).let { restored ->
            restored.copy(missingFields = validateIncidentDraft(restored.toDraft()))
        },
    )
    val uiState = _uiState.asStateFlow()

    fun initialize(incidentId: String?) {
        if (incidentId == null) {
            reduce(IncidentFormStateChange.Initialized(null), persistForm = true, validate = true)
            return
        }
        val current = _uiState.value
        if ((current.isInitialized && current.incidentId == incidentId) || current.loadingIncidentId == incidentId) return
        reduce(IncidentFormStateChange.LookupStarted(incidentId))
        viewModelScope.launch {
            try {
                val incident = getIncident(incidentId)
                if (_uiState.value.loadingIncidentId == incidentId) {
                    reduce(IncidentFormStateChange.Initialized(incident), persistForm = true, validate = true)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reduce(IncidentFormStateChange.LookupFailed(incidentId, error.toIncidentFailure()))
            }
        }
    }

    fun onEvent(event: IncidentFormUiEvent) {
        when (event) {
            is IncidentFormUiEvent.TitleChanged -> {
                reduce(IncidentFormStateChange.TitleChanged(event.value), persistForm = true, validate = true)
            }
            is IncidentFormUiEvent.LocationChanged -> {
                reduce(IncidentFormStateChange.LocationChanged(event.value), persistForm = true, validate = true)
            }
            is IncidentFormUiEvent.DescriptionChanged -> {
                reduce(IncidentFormStateChange.DescriptionChanged(event.value), persistForm = true, validate = true)
            }
            is IncidentFormUiEvent.PhotosSelected -> {
                reduce(IncidentFormStateChange.PhotosSelected(event.uris), persistForm = true, validate = true)
            }
            is IncidentFormUiEvent.PhotoRemoved -> {
                reduce(IncidentFormStateChange.PhotoRemoved(event.photo), persistForm = true, validate = true)
            }
            IncidentFormUiEvent.PhotoInputCancelled -> {
                reduce(IncidentFormStateChange.PhotoInputCancelled, persistForm = true)
            }
            IncidentFormUiEvent.PhotosRemoved -> {
                reduce(IncidentFormStateChange.PhotosRemoved, persistForm = true, validate = true)
            }
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
                    createIncident(request.toDraft())
                } else {
                    updateIncident(
                        Incident(
                            request.incidentId,
                            request.title,
                            request.location,
                            request.description,
                            request.photos,
                        ),
                        request.deletedPhotoIds,
                    )
                }
                reduce(IncidentFormStateChange.SaveSucceeded(saved))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reduce(IncidentFormStateChange.SaveFailed(error.toIncidentFailure()))
            }
        }
    }

    fun clearError() {
        reduce(IncidentFormStateChange.ErrorCleared)
    }

    fun acknowledgeSaved(incident: Incident) {
        reduce(IncidentFormStateChange.SavedAcknowledged(incident))
    }

    private fun reduce(change: IncidentFormStateChange, persistForm: Boolean = false, validate: Boolean = false) {
        _uiState.update { previous ->
            val next = IncidentFormUiStateReducer.reduce(previous, change)
            if (validate) next.copy(missingFields = validateIncidentDraft(next.toDraft())) else next
        }
        if (persistForm) persist(_uiState.value)
    }

    private fun persist(state: IncidentFormUiState) {
        savedStateHandle[INITIALIZED_KEY] = state.isInitialized
        savedStateHandle[INCIDENT_ID_KEY] = state.incidentId
        savedStateHandle[TITLE_KEY] = state.title
        savedStateHandle[LOCATION_KEY] = state.location
        savedStateHandle[DESCRIPTION_KEY] = state.description
        savedStateHandle[PHOTO_TYPES_KEY] = ArrayList(state.photos.map { photo ->
            when (photo) {
                is IncidentPhoto.Local -> "local"
                is IncidentPhoto.Stored -> "stored"
            }
        })
        savedStateHandle[PHOTO_REFERENCES_KEY] = ArrayList(state.photos.map { photo ->
            when (photo) {
                is IncidentPhoto.Local -> photo.uri
                is IncidentPhoto.Stored -> photo.id
            }
        })
        savedStateHandle[PHOTO_DISPLAY_URIS_KEY] = ArrayList(state.photos.map(IncidentPhoto::displayUri))
        savedStateHandle[ORIGINAL_PHOTO_IDS_KEY] = ArrayList(state.originalPhotoIds)
        savedStateHandle[VALIDATED_FIELDS_KEY] = ArrayList(state.validatedFields.map { it.name })
    }
}

private fun IncidentFormUiState.toDraft() =
    IncidentDraft(title, location, description, photos)

private fun restoreFormState(handle: SavedStateHandle): IncidentFormUiState = IncidentFormUiState(
    isInitialized = handle[INITIALIZED_KEY] ?: false,
    incidentId = handle[INCIDENT_ID_KEY],
    title = handle[TITLE_KEY] ?: "",
    location = handle[LOCATION_KEY] ?: "",
    description = handle[DESCRIPTION_KEY] ?: "",
    photos = restorePhotos(handle),
    originalPhotoIds = handle.get<ArrayList<String>>(ORIGINAL_PHOTO_IDS_KEY).orEmpty(),
    validatedFields = handle.get<ArrayList<String>>(VALIDATED_FIELDS_KEY)
        ?.mapNotNull { value -> RequiredIncidentField.entries.firstOrNull { it.name == value } }
        ?.toSet()
        .orEmpty(),
)

private fun restorePhotos(handle: SavedStateHandle): List<IncidentPhoto> {
    val types = handle.get<ArrayList<String>>(PHOTO_TYPES_KEY).orEmpty()
    val references = handle.get<ArrayList<String>>(PHOTO_REFERENCES_KEY).orEmpty()
    val displayUris = handle.get<ArrayList<String>>(PHOTO_DISPLAY_URIS_KEY).orEmpty()
    return types.mapIndexedNotNull { index, type ->
        val reference = references.getOrNull(index) ?: return@mapIndexedNotNull null
        when (type) {
            "local" -> IncidentPhoto.Local(reference)
            "stored" -> displayUris.getOrNull(index)?.let { IncidentPhoto.Stored(reference, it) }
            else -> null
        }
    }
}
