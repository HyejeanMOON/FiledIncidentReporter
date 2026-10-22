package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField

data class IncidentFormUiState(
    val isInitialized: Boolean = false,
    val incidentId: String? = null,
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val photos: List<IncidentPhoto> = emptyList(),
    val loadingIncidentId: String? = null,
    val loadFailure: IncidentFailure? = null,
    val missingFields: List<RequiredIncidentField> = emptyList(),
    val focusedFields: Set<RequiredIncidentField> = emptySet(),
    val validatedFields: Set<RequiredIncidentField> = emptySet(),
    val isSaving: Boolean = false,
    val error: IncidentFailure? = null,
    val savedIncident: Incident? = null,
) {
    val isSubmitEnabled: Boolean
        get() = isInitialized && !isSaving && savedIncident == null && missingFields.isEmpty()
}
