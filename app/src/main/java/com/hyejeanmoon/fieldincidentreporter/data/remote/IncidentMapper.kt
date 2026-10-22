package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentListItemDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentSummary
import java.time.Instant

internal fun IncidentDto.toIncident(photoUrlResolver: RetrofitIncidentPhotoUrlResolver) = Incident(
    id = id,
    title = title,
    location = location,
    description = description,
    photos = photos.orEmpty().map { photo ->
        IncidentPhoto.Stored(photo.id, photoUrlResolver.resolve(photo.url))
    },
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

internal fun IncidentListItemDto.toSummary(photoUrlResolver: RetrofitIncidentPhotoUrlResolver) = IncidentSummary(
    id = id,
    title = title,
    location = location,
    description = description,
    photoPreviews = photos.map { photoUrlResolver.resolve(it.url) },
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)
