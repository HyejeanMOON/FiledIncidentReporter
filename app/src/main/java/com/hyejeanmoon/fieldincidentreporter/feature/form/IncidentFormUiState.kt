package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField

sealed interface IncidentFormUiState {
    /** Loading the report to edit. */
    data object Loading : IncidentFormUiState

    /** The report to edit could not be loaded at all. */
    data class LoadFailed(val failure: IncidentFailure) : IncidentFormUiState

    data class Ready(
        /** Null while creating a new report. */
        val incidentId: String? = null,
        val title: String = "",
        val location: String = "",
        val description: String = "",
        val photos: List<IncidentPhoto> = emptyList(),
        val originalPhotoIds: List<String> = emptyList(),
        val missingFields: List<RequiredIncidentField> = emptyList(),
        val focusedFields: Set<RequiredIncidentField> = emptySet(),
        /** Fields the user has visited; only these show a "required" error. */
        val validatedFields: Set<RequiredIncidentField> = emptySet(),
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        /** Shown as a dialog over the form. */
        val error: IncidentFailure? = null,
    ) : IncidentFormUiState {
        val isEditing: Boolean get() = incidentId != null

        val canEdit: Boolean get() = !isSaving && !isSaved

        val isSubmitEnabled: Boolean get() = canEdit && missingFields.isEmpty()

        val visibleMissingFields: List<RequiredIncidentField> get() = missingFields.filter { it in validatedFields }

        val deletedPhotoIds: List<String>
            get() {
                val retainedIds = photos.filterIsInstance<IncidentPhoto.Stored>().map(IncidentPhoto.Stored::id).toSet()
                return originalPhotoIds.filterNot(retainedIds::contains)
            }

        fun toDraft() = IncidentDraft(title, location, description, photos)
    }
}

sealed interface IncidentFormUiEvent {
    data class TitleChanged(val value: String) : IncidentFormUiEvent
    data class LocationChanged(val value: String) : IncidentFormUiEvent
    data class DescriptionChanged(val value: String) : IncidentFormUiEvent
    data class PhotosSelected(val uris: List<String>) : IncidentFormUiEvent
    data class PhotoRemoved(val photo: IncidentPhoto) : IncidentFormUiEvent
    data object PhotosRemoved : IncidentFormUiEvent
    data object PhotoInputCancelled : IncidentFormUiEvent
    data class FieldFocusChanged(val field: RequiredIncidentField, val isFocused: Boolean) : IncidentFormUiEvent
    data object Submit : IncidentFormUiEvent
    data object Retry : IncidentFormUiEvent
    data object ErrorDismissed : IncidentFormUiEvent
}
