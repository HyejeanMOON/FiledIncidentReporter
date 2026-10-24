package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import okhttp3.HttpUrl
import java.time.Instant

internal fun IncidentDto.toIncident(baseUrl: HttpUrl) = Incident(
    id = id,
    title = title,
    location = location,
    description = description,
    photos = photos
        .filter { it.id.isNotBlank() }
        .map { photo ->
            IncidentPhoto.Stored(
                id = photo.id,
                displayUri = baseUrl.resolveOrSelf(photo.url),
                thumbnailUri = baseUrl.resolveOrSelf(photo.thumbnailUrl.ifBlank { photo.url }),
            )
        },
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    resolvedAt = resolvedAt?.let(Instant::parse),
)

/** The server returns photo paths relative to its origin. */
internal fun HttpUrl.resolveOrSelf(path: String): String = resolve(path)?.toString() ?: path
