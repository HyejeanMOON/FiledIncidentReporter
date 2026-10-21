package com.hyejeanmoon.fieldincidentreporter.feature.form

sealed interface IncidentFormUiEvent {
    data class TitleChanged(val value: String) : IncidentFormUiEvent
    data class LocationChanged(val value: String) : IncidentFormUiEvent
    data class DescriptionChanged(val value: String) : IncidentFormUiEvent
    data class PhotosSelected(val uris: List<String>) : IncidentFormUiEvent
    data object PhotosRemoved : IncidentFormUiEvent
    data object Submit : IncidentFormUiEvent
}
