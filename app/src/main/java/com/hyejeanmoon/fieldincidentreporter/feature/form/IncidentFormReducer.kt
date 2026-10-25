package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_DESCRIPTION_LENGTH
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_LOCATION_LENGTH
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_TITLE_LENGTH
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import javax.inject.Inject

/**
 * Computes each form state from the previous one. It only marks loading or saving as started;
 * [IncidentFormViewModel] does that work and reports back with an [IncidentFormResult].
 */
class IncidentFormReducer @Inject constructor() {
    fun reduce(state: IncidentFormUiState, event: IncidentFormUiEvent): IncidentFormUiState = when (state) {
        IncidentFormUiState.Loading -> state
        is IncidentFormUiState.LoadFailed -> if (event == IncidentFormUiEvent.Retry) IncidentFormUiState.Loading else state
        is IncidentFormUiState.Ready -> state.reduce(event)
    }

    fun reduce(state: IncidentFormUiState, result: IncidentFormResult): IncidentFormUiState = when (result) {
        is IncidentFormResult.Loaded -> formOf(result.incident)
        is IncidentFormResult.LoadFailed -> IncidentFormUiState.LoadFailed(result.failure)
        IncidentFormResult.Saved -> state.ifReady { copy(isSaving = false, isSaved = true) }
        is IncidentFormResult.SaveFailed -> state.ifReady { copy(isSaving = false, error = result.failure) }
    }
}

private fun IncidentFormUiState.Ready.reduce(event: IncidentFormUiEvent): IncidentFormUiState.Ready = when (event) {
    // Typing or pasting stops at the field's limit, as an HTML `maxlength` does.
    is IncidentFormUiEvent.TitleChanged -> edit { copy(title = event.value.take(MAX_INCIDENT_TITLE_LENGTH)) }
    is IncidentFormUiEvent.LocationChanged -> edit { copy(location = event.value.take(MAX_INCIDENT_LOCATION_LENGTH)) }
    is IncidentFormUiEvent.DescriptionChanged -> edit { copy(description = event.value.take(MAX_INCIDENT_DESCRIPTION_LENGTH)) }
    // Photos are added with a button, not a focusable field, so choosing them is what reveals their error.
    is IncidentFormUiEvent.PhotosSelected -> edit {
        copy(photos = (photos + event.uris.filter(String::isNotBlank).map(IncidentPhoto::Local)).distinct()).photosVisited()
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
