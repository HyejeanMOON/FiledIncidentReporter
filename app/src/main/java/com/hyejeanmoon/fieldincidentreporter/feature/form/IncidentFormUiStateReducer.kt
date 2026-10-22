package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField

internal sealed interface IncidentFormStateChange {
    data class LookupStarted(val id: String) : IncidentFormStateChange
    data class LookupFailed(val id: String, val failure: IncidentFailure) : IncidentFormStateChange
    data class Initialized(val incident: Incident?) : IncidentFormStateChange
    data class TitleChanged(val value: String) : IncidentFormStateChange
    data class LocationChanged(val value: String) : IncidentFormStateChange
    data class DescriptionChanged(val value: String) : IncidentFormStateChange
    data class PhotosSelected(val uris: List<String>) : IncidentFormStateChange
    data class PhotoRemoved(val photo: IncidentPhoto) : IncidentFormStateChange
    data object PhotoInputCancelled : IncidentFormStateChange
    data object PhotosRemoved : IncidentFormStateChange
    data class FieldFocusChanged(val field: RequiredIncidentField, val isFocused: Boolean) : IncidentFormStateChange
    data object SaveStarted : IncidentFormStateChange
    data class SaveSucceeded(val incident: Incident) : IncidentFormStateChange
    data class SaveFailed(val failure: IncidentFailure) : IncidentFormStateChange
    data object ErrorCleared : IncidentFormStateChange
    data class SavedAcknowledged(val incident: Incident) : IncidentFormStateChange
}

internal object IncidentFormUiStateReducer {
    fun reduce(previous: IncidentFormUiState, change: IncidentFormStateChange): IncidentFormUiState = when (change) {
        is IncidentFormStateChange.LookupStarted -> previous.copy(
            loadingIncidentId = change.id,
            loadFailure = null,
        )
        is IncidentFormStateChange.LookupFailed -> if (previous.loadingIncidentId == change.id) {
            previous.copy(loadingIncidentId = null, loadFailure = change.failure)
        } else previous
        is IncidentFormStateChange.Initialized -> if (previous.isInitialized) previous else IncidentFormUiState(
            isInitialized = true,
            incidentId = change.incident?.id,
            title = change.incident?.title.orEmpty(),
            location = change.incident?.location.orEmpty(),
            description = change.incident?.description.orEmpty(),
            photos = change.incident?.photos.orEmpty(),
            originalPhotoIds = change.incident?.photos.orEmpty()
                .filterIsInstance<IncidentPhoto.Stored>().map(IncidentPhoto.Stored::id),
        )
        is IncidentFormStateChange.TitleChanged -> previous.edit { copy(title = change.value) }
        is IncidentFormStateChange.LocationChanged -> previous.edit { copy(location = change.value) }
        is IncidentFormStateChange.DescriptionChanged -> previous.edit { copy(description = change.value) }
        is IncidentFormStateChange.PhotosSelected -> if (change.uris.isEmpty()) previous else {
            previous.edit { copy(photos = (photos + change.uris.map(IncidentPhoto::Local)).distinct()) }
        }
        is IncidentFormStateChange.PhotoRemoved -> previous.edit {
            copy(photos = photos - change.photo, validatedFields = validatedFields + RequiredIncidentField.PHOTOS)
        }
        IncidentFormStateChange.PhotoInputCancelled -> previous.edit {
            if (photos.isEmpty()) {
                copy(validatedFields = validatedFields + RequiredIncidentField.PHOTOS)
            } else {
                this
            }
        }
        IncidentFormStateChange.PhotosRemoved -> previous.edit {
            copy(
                photos = emptyList(),
                validatedFields = validatedFields + RequiredIncidentField.PHOTOS,
            )
        }
        is IncidentFormStateChange.FieldFocusChanged -> if (change.isFocused) {
            previous.copy(focusedFields = previous.focusedFields + change.field)
        } else if (change.field in previous.focusedFields) {
            previous.copy(
                focusedFields = previous.focusedFields - change.field,
                validatedFields = previous.validatedFields + change.field,
            )
        } else previous
        IncidentFormStateChange.SaveStarted -> if (previous.isSubmitEnabled) {
            previous.copy(isSaving = true, error = null)
        } else previous
        is IncidentFormStateChange.SaveSucceeded -> previous.copy(
            savedIncident = change.incident,
            isSaving = false,
        )
        is IncidentFormStateChange.SaveFailed -> previous.copy(error = change.failure, isSaving = false)
        IncidentFormStateChange.ErrorCleared -> previous.copy(error = null)
        is IncidentFormStateChange.SavedAcknowledged -> if (previous.savedIncident == change.incident) {
            previous.copy(savedIncident = null)
        } else previous
    }

    private fun IncidentFormUiState.canEdit(): Boolean =
        isInitialized && !isSaving && savedIncident == null

    private inline fun IncidentFormUiState.edit(change: IncidentFormUiState.() -> IncidentFormUiState): IncidentFormUiState =
        if (canEdit()) change() else this
}
