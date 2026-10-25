package com.hyejeanmoon.fieldincidentreporter.feature.detail

sealed interface IncidentDetailUiEvent {
    data object Retry : IncidentDetailUiEvent
    data object Reload : IncidentDetailUiEvent
    data object Delete : IncidentDetailUiEvent
    data object ErrorDismissed : IncidentDetailUiEvent
}
