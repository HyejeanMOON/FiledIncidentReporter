package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.missingRequiredFields

internal sealed interface IncidentFormStateChange {
    data class Initialized(val incident: Incident?) : IncidentFormStateChange
    data class TitleChanged(val value: String) : IncidentFormStateChange
    data class LocationChanged(val value: String) : IncidentFormStateChange
    data class DescriptionChanged(val value: String) : IncidentFormStateChange
    data class PhotosSelected(val uris: List<String>) : IncidentFormStateChange
    data object PhotosRemoved : IncidentFormStateChange
    data object SaveStarted : IncidentFormStateChange
    data class SaveSucceeded(val incident: Incident) : IncidentFormStateChange
    data class SaveFailed(val failure: IncidentFailure) : IncidentFormStateChange
    data object SaveFinished : IncidentFormStateChange
    data object ErrorCleared : IncidentFormStateChange
    data class SavedAcknowledged(val incident: Incident) : IncidentFormStateChange
    data object Restored : IncidentFormStateChange
}

internal object IncidentFormUiStateReducer {
    fun reduce(previous: IncidentFormUiState, change: IncidentFormStateChange): IncidentFormUiState = when (change) {
        is IncidentFormStateChange.Initialized -> if (previous.isInitialized) previous else IncidentFormUiState(
            isInitialized = true,
            incidentId = change.incident?.id,
            title = change.incident?.title.orEmpty(),
            location = change.incident?.location.orEmpty(),
            description = change.incident?.description.orEmpty(),
            photoUris = change.incident?.photoUris.orEmpty(),
        ).withMissingFields()
        is IncidentFormStateChange.TitleChanged -> previous.edit { copy(title = change.value) }
        is IncidentFormStateChange.LocationChanged -> previous.edit { copy(location = change.value) }
        is IncidentFormStateChange.DescriptionChanged -> previous.edit { copy(description = change.value) }
        is IncidentFormStateChange.PhotosSelected -> if (change.uris.isEmpty()) previous else {
            previous.edit { copy(photoUris = change.uris) }
        }
        IncidentFormStateChange.PhotosRemoved -> previous.edit { copy(photoUris = emptyList()) }
        IncidentFormStateChange.SaveStarted -> if (previous.isSubmitEnabled) {
            previous.copy(isSaving = true, error = null)
        } else previous
        is IncidentFormStateChange.SaveSucceeded -> previous.copy(
            savedIncident = change.incident,
            isSaving = false,
        )
        is IncidentFormStateChange.SaveFailed -> previous.copy(error = change.failure, isSaving = false)
        IncidentFormStateChange.SaveFinished -> previous.copy(isSaving = false)
        IncidentFormStateChange.ErrorCleared -> previous.copy(error = null)
        is IncidentFormStateChange.SavedAcknowledged -> if (previous.savedIncident == change.incident) {
            previous.copy(savedIncident = null)
        } else previous
        IncidentFormStateChange.Restored -> previous.withMissingFields()
    }

    private fun IncidentFormUiState.canEdit(): Boolean =
        isInitialized && !isSaving && savedIncident == null

    private inline fun IncidentFormUiState.edit(change: IncidentFormUiState.() -> IncidentFormUiState): IncidentFormUiState =
        if (canEdit()) change().withMissingFields() else this

    private fun IncidentFormUiState.withMissingFields(): IncidentFormUiState = copy(
        missingFields = missingRequiredFields(title, location, description, photoUris),
    )
}
