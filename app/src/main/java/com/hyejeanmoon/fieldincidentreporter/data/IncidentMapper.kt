package com.hyejeanmoon.fieldincidentreporter.data

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

internal fun IncidentDto.toIncident() = Incident(id, title, location, description, photoUris.orEmpty())

internal fun Incident.toDto() = IncidentDto(id, title, location, description, photoUris)
