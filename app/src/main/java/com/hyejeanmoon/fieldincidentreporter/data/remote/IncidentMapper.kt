package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

internal fun IncidentDto.toIncident() = Incident(id, title, location, description, photoUris.orEmpty())
