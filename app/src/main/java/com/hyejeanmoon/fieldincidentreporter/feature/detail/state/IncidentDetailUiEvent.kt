package com.hyejeanmoon.fieldincidentreporter.feature.detail.state

sealed interface IncidentDetailUiEvent {
    data object Retry : IncidentDetailUiEvent
    data object Reload : IncidentDetailUiEvent
    data object Delete : IncidentDetailUiEvent
    data object ErrorDismissed : IncidentDetailUiEvent
}
