package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import com.hyejeanmoon.fieldincidentreporter.domain.error.toIncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.SaveIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ValidateIncidentDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class IncidentFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getIncident: GetIncidentUseCase,
    private val saveIncident: SaveIncidentUseCase,
    private val validateIncidentDraft: ValidateIncidentDraftUseCase,
) : ViewModel() {
    private val incidentId = savedStateHandle.toIncidentFormRoute().incidentId
    private val _uiState = MutableStateFlow(
        restoreForm()?.validated() ?: IncidentFormUiState(incidentId = incidentId, isLoading = incidentId != null).validated(),
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (_uiState.value.isLoading) load()
    }

    fun onEvent(event: IncidentFormUiEvent) {
        when (event) {
            is IncidentFormUiEvent.TitleChanged -> edit { copy(title = event.value) }
            is IncidentFormUiEvent.LocationChanged -> edit { copy(location = event.value) }
            is IncidentFormUiEvent.DescriptionChanged -> edit { copy(description = event.value) }
            is IncidentFormUiEvent.PhotosSelected -> edit {
                copy(photos = (photos + event.uris.filter(String::isNotBlank).map(IncidentPhoto::Local)).distinct())
            }
            is IncidentFormUiEvent.PhotoRemoved -> edit { copy(photos = photos - event.photo).photosVisited() }
            IncidentFormUiEvent.PhotosRemoved -> edit { copy(photos = emptyList()).photosVisited() }
            IncidentFormUiEvent.PhotoInputCancelled -> edit { if (photos.isEmpty()) photosVisited() else this }
            is IncidentFormUiEvent.FieldFocusChanged -> onFocusChanged(event.field, event.isFocused)
            IncidentFormUiEvent.Submit -> submit()
            IncidentFormUiEvent.Retry -> load()
            IncidentFormUiEvent.ErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun load() {
        val id = incidentId ?: return
        _uiState.update { it.copy(isLoading = true, loadFailure = null) }
        viewModelScope.launch {
            runSuspendCatching { getIncident(id) }
                .onSuccess { incident -> setForm(_uiState.value.filledWith(incident)) }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, loadFailure = error.toIncidentFailure()) }
                }
        }
    }

    private fun onFocusChanged(field: RequiredIncidentField, isFocused: Boolean) {
        val state = _uiState.value
        when {
            isFocused -> _uiState.update { it.copy(focusedFields = it.focusedFields + field) }
            // Leaving a field reveals its validation error.
            field in state.focusedFields -> setForm(
                state.copy(focusedFields = state.focusedFields - field, validatedFields = state.validatedFields + field),
            )
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.isSubmitEnabled) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runSuspendCatching { saveIncident(state.incidentId, state.toDraft(), state.deletedPhotoIds) }
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.toIncidentFailure()) }
                }
        }
    }

    private inline fun edit(transform: IncidentFormUiState.() -> IncidentFormUiState) {
        val state = _uiState.value
        if (state.canEdit) setForm(state.transform())
    }

    /** Publishes [state] after validating it, and saves it so that it survives process death. */
    private fun setForm(state: IncidentFormUiState) {
        _uiState.value = state.validated()
        savedStateHandle[SAVED_FORM_KEY] = Json.encodeToString(SavedForm.from(state))
    }

    private fun IncidentFormUiState.validated() = copy(missingFields = validateIncidentDraft(toDraft()))

    private fun restoreForm(): IncidentFormUiState? = savedStateHandle.get<String>(SAVED_FORM_KEY)
        ?.let { runCatching { Json.decodeFromString<SavedForm>(it) }.getOrNull() }
        ?.toUiState(incidentId)

    companion object {
        internal const val SAVED_FORM_KEY = "incident_form"
    }
}

private fun IncidentFormUiState.photosVisited() =
    copy(validatedFields = validatedFields + RequiredIncidentField.PHOTOS)

private fun IncidentFormUiState.filledWith(incident: Incident) = copy(
    isLoading = false,
    title = incident.title,
    location = incident.location,
    description = incident.description,
    photos = incident.photos,
    originalPhotoIds = incident.photos.filterIsInstance<IncidentPhoto.Stored>().map(IncidentPhoto.Stored::id),
)

/** The user's edits, kept in [SavedStateHandle] as JSON. */
@Serializable
internal data class SavedForm(
    val title: String,
    val location: String,
    val description: String,
    val photos: List<SavedPhoto>,
    val originalPhotoIds: List<String>,
    val validatedFields: Set<RequiredIncidentField>,
) {
    /** A [storedId] marks a photo already on the server; otherwise [uri] is a local photo. */
    @Serializable
    data class SavedPhoto(val uri: String, val storedId: String? = null)

    fun toUiState(incidentId: String?) = IncidentFormUiState(
        incidentId = incidentId,
        title = title,
        location = location,
        description = description,
        photos = photos.map { photo ->
            photo.storedId?.let { IncidentPhoto.Stored(it, photo.uri) } ?: IncidentPhoto.Local(photo.uri)
        },
        originalPhotoIds = originalPhotoIds,
        validatedFields = validatedFields,
    )

    companion object {
        fun from(state: IncidentFormUiState) = SavedForm(
            title = state.title,
            location = state.location,
            description = state.description,
            photos = state.photos.map { photo ->
                when (photo) {
                    is IncidentPhoto.Local -> SavedPhoto(photo.uri)
                    is IncidentPhoto.Stored -> SavedPhoto(photo.displayUri, photo.id)
                }
            },
            originalPhotoIds = state.originalPhotoIds,
            validatedFields = state.validatedFields,
        )
    }
}
