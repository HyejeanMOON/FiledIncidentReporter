package com.hyejeanmoon.fieldincidentreporter.feature.list

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

data class IncidentListUiState(
    val selectedIncident: Incident? = null,
    val loadingIncidentId: String? = null,
    val failedIncidentId: String? = null,
    val lookupFailure: IncidentFailure? = null,
    val lookupDialogVisible: Boolean = false,
    val deletedFromEntryId: String? = null,
)
