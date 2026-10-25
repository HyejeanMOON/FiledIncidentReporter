package com.hyejeanmoon.fieldincidentreporter.feature.form.state

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

/** The outcome of loading or saving, fed back into [IncidentFormReducer]. */
sealed interface IncidentFormResult {
    data class Loaded(val incident: Incident) : IncidentFormResult
    data class LoadFailed(val failure: IncidentFailure) : IncidentFormResult
    data object Saved : IncidentFormResult
    data class SaveFailed(val failure: IncidentFailure) : IncidentFormResult
}
