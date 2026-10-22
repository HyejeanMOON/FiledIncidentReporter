package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentListItemDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentSummary

internal fun IncidentDto.toIncident(photoUrlResolver: RetrofitIncidentPhotoUrlResolver) = Incident(
    id = id,
    title = title,
    location = location,
    description = description,
    photos = photoUris.orEmpty().map { path ->
        IncidentPhoto.Stored(path, photoUrlResolver.resolve(path))
    },
)

internal fun IncidentListItemDto.toSummary() = IncidentSummary(
    id = id,
    title = title,
    location = location,
    description = description,
    photoPreviews = photos,
)
