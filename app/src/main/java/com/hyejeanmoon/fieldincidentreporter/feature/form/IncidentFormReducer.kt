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
    fun reduce(state: IncidentFormUiState, event: IncidentFormUiEvent): IncidentFormUiState = when (event) {
        is IncidentFormUiEvent.TitleChanged -> state.edit { copy(title = event.value) }
        is IncidentFormUiEvent.LocationChanged -> state.edit { copy(location = event.value) }
        is IncidentFormUiEvent.DescriptionChanged -> state.edit { copy(description = event.value) }
        is IncidentFormUiEvent.PhotosSelected -> state.edit {
            copy(photos = (photos + event.uris.filter(String::isNotBlank).map(IncidentPhoto::Local)).distinct())
        }
        is IncidentFormUiEvent.PhotoRemoved -> state.edit { copy(photos = photos - event.photo).photosVisited() }
        IncidentFormUiEvent.PhotosRemoved -> state.edit { copy(photos = emptyList()).photosVisited() }
        IncidentFormUiEvent.PhotoInputCancelled -> state.edit { if (photos.isEmpty()) photosVisited() else this }
        is IncidentFormUiEvent.FieldFocusChanged -> when {
            event.isFocused -> state.copy(focusedFields = state.focusedFields + event.field)
            // Leaving a field reveals its validation error.
            event.field in state.focusedFields -> state.copy(
                focusedFields = state.focusedFields - event.field,
                validatedFields = state.validatedFields + event.field,
            )
            else -> state
        }
        IncidentFormUiEvent.Submit -> if (state.isSubmitEnabled) state.copy(isSaving = true, error = null) else state
        IncidentFormUiEvent.Retry -> if (state.loadFailure != null) state.copy(isLoading = true, loadFailure = null) else state
        IncidentFormUiEvent.ErrorDismissed -> state.copy(error = null)
    }.let(::validated)

    fun reduce(state: IncidentFormUiState, result: IncidentFormResult): IncidentFormUiState = when (result) {
        is IncidentFormResult.Loaded -> state.filledWith(result.incident)
        is IncidentFormResult.LoadFailed -> state.copy(isLoading = false, loadFailure = result.failure)
        IncidentFormResult.Saved -> state.copy(isSaving = false, isSaved = true)
        is IncidentFormResult.SaveFailed -> state.copy(isSaving = false, error = result.failure)
    }.let(::validated)

    fun validated(state: IncidentFormUiState) = state.copy(missingFields = validateIncidentDraft(state.toDraft()))
}

private inline fun IncidentFormUiState.edit(transform: IncidentFormUiState.() -> IncidentFormUiState) =
    if (canEdit) transform() else this

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
