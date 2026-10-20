package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

data class IncidentFormEvent(val originEntryId: String, val savedIncident: Incident)
