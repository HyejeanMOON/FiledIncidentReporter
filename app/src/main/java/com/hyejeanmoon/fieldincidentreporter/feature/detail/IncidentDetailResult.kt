package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

/** The outcome of loading or deleting, fed back into [IncidentDetailReducer]. */
sealed interface IncidentDetailResult {
    data class Loaded(val incident: Incident) : IncidentDetailResult
    data class LoadFailed(val failure: IncidentFailure) : IncidentDetailResult
    data object Deleted : IncidentDetailResult
    data class DeleteFailed(val failure: IncidentFailure) : IncidentDetailResult
}
