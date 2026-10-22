package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPage
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import okhttp3.HttpUrl
import java.time.Instant

internal fun IncidentDto.toIncident(baseUrl: HttpUrl) = Incident(
    id = id,
    title = title,
    location = location,
    description = description,
    photos = photos.orEmpty().map { photo -> IncidentPhoto.Stored(photo.id, baseUrl.resolveOrSelf(photo.url)) },
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

internal fun IncidentPageDto.toIncidentPage(baseUrl: HttpUrl) =
    IncidentPage(items.map { it.toIncident(baseUrl) }, page, hasNext)

/** The server returns photo paths relative to its origin. */
internal fun HttpUrl.resolveOrSelf(path: String): String = resolve(path)?.toString() ?: path
