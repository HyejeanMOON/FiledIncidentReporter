package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField

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
