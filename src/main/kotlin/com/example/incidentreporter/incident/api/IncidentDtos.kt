package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.IncidentPhoto
import com.example.incidentreporter.incident.domain.IncomingPhoto
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

/** Editable text fields shared by the create and update request bodies. */
interface IncidentFields {
    val title: String
    val location: String
    val description: String
}

fun IncidentFields.toInput() = IncidentInput(title, location, description)

data class IncidentBody(
    override val title: String,
    override val location: String,
    override val description: String,
) : IncidentFields

data class IncidentUpdateBody(
    override val title: String,
    override val location: String,
    override val description: String,
    val deletePhotoIds: List<String> = emptyList(),
) : IncidentFields

data class IncidentResolutionBody(val resolved: Boolean)

data class PhotoResponse(val id: String, val url: String, val thumbnailUrl: String)

data class IncidentResponse(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photos: List<PhotoResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resolved: Boolean,
    val resolvedAt: Instant?,
)

data class IncidentPageResponse(
    val items: List<IncidentResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

fun MultipartFile.toIncomingPhoto() = IncomingPhoto(contentType, size, this::getInputStream)

private fun IncidentPhoto.toResponse(incidentId: String): PhotoResponse {
    val url = "/incidents/$incidentId/photos/$id"
    return PhotoResponse(id, url, "$url/thumbnail")
}

fun Incident.toResponse() = IncidentResponse(
    id, title, location, description, photos.map { it.toResponse(id) }, createdAt, updatedAt,
    resolvedAt != null, resolvedAt,
)

fun IncidentPage.toResponse() = IncidentPageResponse(items.map(Incident::toResponse), page, size, hasNext)
