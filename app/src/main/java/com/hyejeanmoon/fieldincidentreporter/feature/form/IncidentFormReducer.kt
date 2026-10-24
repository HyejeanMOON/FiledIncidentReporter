package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ValidateIncidentDraftUseCase
import javax.inject.Inject

/** The outcome of loading or saving, fed back into [IncidentFormReducer]. */
sealed interface IncidentFormResult {
    data class Loaded(val incident: Incident) : IncidentFormResult
    data class LoadFailed(val failure: IncidentFailure) : IncidentFormResult
    data object Saved : IncidentFormResult
    data class SaveFailed(val failure: IncidentFailure) : IncidentFormResult
}

/**
 * Computes each form state from the previous one. It only marks loading or saving as started;
 * [IncidentFormViewModel] does that work and reports back with an [IncidentFormResult].
 */
class IncidentFormReducer @Inject constructor(
    private val validateIncidentDraft: ValidateIncidentDraftUseCase,
) {
    fun reduce(state: IncidentFormUiState, event: IncidentFormUiEvent): IncidentFormUiState = when (state) {
        IncidentFormUiState.Loading -> state
        is IncidentFormUiState.LoadFailed -> if (event == IncidentFormUiEvent.Retry) IncidentFormUiState.Loading else state
        is IncidentFormUiState.Ready -> validated(state.reduce(event))
    }

    fun reduce(state: IncidentFormUiState, result: IncidentFormResult): IncidentFormUiState = when (result) {
        is IncidentFormResult.Loaded -> validated(formOf(result.incident))
        is IncidentFormResult.LoadFailed -> IncidentFormUiState.LoadFailed(result.failure)
        IncidentFormResult.Saved -> state.ifReady { copy(isSaving = false, isSaved = true) }
        is IncidentFormResult.SaveFailed -> state.ifReady { copy(isSaving = false, error = result.failure) }
    }

    fun validated(state: IncidentFormUiState.Ready) = state.copy(missingFields = validateIncidentDraft(state.toDraft()))
}

private fun IncidentFormUiState.Ready.reduce(event: IncidentFormUiEvent): IncidentFormUiState.Ready = when (event) {
    is IncidentFormUiEvent.TitleChanged -> edit { copy(title = event.value) }
    is IncidentFormUiEvent.LocationChanged -> edit { copy(location = event.value) }
    is IncidentFormUiEvent.DescriptionChanged -> edit { copy(description = event.value) }
    is IncidentFormUiEvent.PhotosSelected -> edit {
        copy(photos = (photos + event.uris.filter(String::isNotBlank).map(IncidentPhoto::Local)).distinct())
    }
    is IncidentFormUiEvent.PhotoRemoved -> edit { copy(photos = photos - event.photo).photosVisited() }
    IncidentFormUiEvent.PhotosRemoved -> edit { copy(photos = emptyList()).photosVisited() }
    IncidentFormUiEvent.PhotoInputCancelled -> edit { if (photos.isEmpty()) photosVisited() else this }
    is IncidentFormUiEvent.FieldFocusChanged -> when {
        event.isFocused -> copy(focusedFields = focusedFields + event.field)
        // Leaving a field reveals its validation error.
        event.field in focusedFields -> copy(
            focusedFields = focusedFields - event.field,
            validatedFields = validatedFields + event.field,
        )
        else -> this
    }
    IncidentFormUiEvent.Submit -> if (isSubmitEnabled) copy(isSaving = true, error = null) else this
    IncidentFormUiEvent.Retry -> this
    IncidentFormUiEvent.ErrorDismissed -> copy(error = null)
}

private inline fun IncidentFormUiState.Ready.edit(transform: IncidentFormUiState.Ready.() -> IncidentFormUiState.Ready) =
    if (canEdit) transform() else this

private inline fun IncidentFormUiState.ifReady(
    transform: IncidentFormUiState.Ready.() -> IncidentFormUiState,
) = if (this is IncidentFormUiState.Ready) transform() else this

private fun IncidentFormUiState.Ready.photosVisited() =
    copy(validatedFields = validatedFields + RequiredIncidentField.PHOTOS)

private fun formOf(incident: Incident) = IncidentFormUiState.Ready(
    incidentId = incident.id,
    title = incident.title,
    location = incident.location,
    description = incident.description,
    photos = incident.photos,
    originalPhotoIds = incident.photos.filterIsInstance<IncidentPhoto.Stored>().map(IncidentPhoto.Stored::id),
)
