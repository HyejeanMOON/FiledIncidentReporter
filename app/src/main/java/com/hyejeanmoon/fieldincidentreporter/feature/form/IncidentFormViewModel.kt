package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import com.hyejeanmoon.fieldincidentreporter.domain.error.toIncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.domain.photo.CapturedPhotoStore
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.SaveIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.feature.form.state.IncidentFormReducer
import com.hyejeanmoon.fieldincidentreporter.feature.form.state.IncidentFormResult
import com.hyejeanmoon.fieldincidentreporter.feature.form.state.IncidentFormUiEvent
import com.hyejeanmoon.fieldincidentreporter.feature.form.state.IncidentFormUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class IncidentFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: IncidentRepository,
    private val saveIncident: SaveIncidentUseCase,
    private val reducer: IncidentFormReducer,
    private val capturedPhotos: CapturedPhotoStore,
) : ViewModel() {
    private val incidentId = savedStateHandle.toIncidentFormRoute().incidentId
    private val _uiState = MutableStateFlow(
        restoreForm() ?: if (incidentId != null) IncidentFormUiState.Loading else IncidentFormUiState.Ready(),
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (_uiState.value == IncidentFormUiState.Loading) load()
    }

    fun onEvent(event: IncidentFormUiEvent) {
        val previous = _uiState.value
        val state = reducer.reduce(previous, event)
        setState(state)
        // The reducer only marks the work as started, and leaves the state alone when it should not start.
        if (state != previous) when (event) {
            IncidentFormUiEvent.Retry -> load()
            IncidentFormUiEvent.Submit -> save(state as IncidentFormUiState.Ready)
            else -> Unit
        }
    }

    fun newCapturedPhotoUri(): String = capturedPhotos.newPhotoUri()

    /**
     * The form has closed: a saved report's photos are on the server, and a cancelled report's are not needed.
     * This is not called on process death, so a restored form keeps its captured photos.
     */
    override fun onCleared() {
        capturedPhotos.clear()
    }

    private fun load() {
        val id = incidentId ?: return
        viewModelScope.launch {
            runSuspendCatching { repository.getIncident(id) }
                .onSuccess { onResult(IncidentFormResult.Loaded(it)) }
                .onFailure { onResult(IncidentFormResult.LoadFailed(it.toIncidentFailure())) }
        }
    }

    private fun save(state: IncidentFormUiState.Ready) {
        viewModelScope.launch {
            runSuspendCatching { saveIncident(state.incidentId, state.toDraft(), state.deletedPhotoIds) }
                .onSuccess { onResult(IncidentFormResult.Saved) }
                .onFailure { onResult(IncidentFormResult.SaveFailed(it.toIncidentFailure())) }
        }
    }

    private fun onResult(result: IncidentFormResult) = setState(reducer.reduce(_uiState.value, result))

    /** Publishes [state], and saves the user's edits so that they survive process death. */
    private fun setState(state: IncidentFormUiState) {
        val form = (state as? IncidentFormUiState.Ready)?.let(SavedForm::from)
        val previousForm = (_uiState.value as? IncidentFormUiState.Ready)?.let(SavedForm::from)
        if (form != null && form != previousForm) savedStateHandle[SAVED_FORM_KEY] = Json.encodeToString(form)
        _uiState.value = state
    }

    private fun restoreForm(): IncidentFormUiState.Ready? = savedStateHandle.get<String>(SAVED_FORM_KEY)
        ?.let { runCatching { Json.decodeFromString<SavedForm>(it) }.getOrNull() }
        ?.toUiState(incidentId)

    companion object {
        internal const val SAVED_FORM_KEY = "incident_form"
    }
}

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
    data class SavedPhoto(val uri: String, val storedId: String? = null, val thumbnailUri: String? = null)

    fun toUiState(incidentId: String?) = IncidentFormUiState.Ready(
        incidentId = incidentId,
        title = title,
        location = location,
        description = description,
        photos = photos.map { photo ->
            photo.storedId?.let { IncidentPhoto.Stored(it, photo.uri, photo.thumbnailUri ?: photo.uri) }
                ?: IncidentPhoto.Local(photo.uri)
        },
        originalPhotoIds = originalPhotoIds,
        validatedFields = validatedFields,
    )

    companion object {
        fun from(state: IncidentFormUiState.Ready) = SavedForm(
            title = state.title,
            location = state.location,
            description = state.description,
            photos = state.photos.map { photo ->
                when (photo) {
                    is IncidentPhoto.Local -> SavedPhoto(photo.uri)
                    is IncidentPhoto.Stored -> SavedPhoto(photo.displayUri, photo.id, photo.thumbnailUri)
                }
            },
            originalPhotoIds = state.originalPhotoIds,
            validatedFields = state.validatedFields,
        )
    }
}
