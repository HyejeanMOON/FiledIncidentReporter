package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.IncidentPhoto
import com.example.incidentreporter.incident.domain.IncidentPosition
import com.example.incidentreporter.incident.domain.IncidentSearchField
import com.example.incidentreporter.incident.domain.IncidentSummary
import com.example.incidentreporter.incident.domain.IncomingPhoto
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.Base64

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

/**
 * [page] is the requested page number, or null for a cursor request. [nextCursor] is passed back as
 * `cursor` for the incidents after [items]; it is null on the last page, where [hasNext] is false.
 */
data class IncidentPageResponse(
    val items: List<IncidentResponse>,
    val page: Int?,
    val size: Int,
    val hasNext: Boolean,
    val nextCursor: String?,
)

data class IncidentSummaryResponse(val total: Int, val open: Int, val resolved: Int)

fun MultipartFile.toIncomingPhoto() = IncomingPhoto(contentType, size, this::getInputStream)

private fun IncidentPhoto.toResponse(incidentId: String): PhotoResponse {
    val url = "/incidents/$incidentId/photos/$id"
    return PhotoResponse(id, url, "$url/thumbnail")
}

fun Incident.toResponse() = IncidentResponse(
    id, title, location, description, photos.map { it.toResponse(id) }, createdAt, updatedAt,
    resolvedAt != null, resolvedAt,
)

fun IncidentSummary.toResponse() = IncidentSummaryResponse(total, open, resolved)

fun IncidentPage.toResponse(page: Int?, size: Int) =
    IncidentPageResponse(items.map(Incident::toResponse), page, size, next != null, next?.toCursor())

/** Cursors are opaque to clients: the base64url form of a list position. */
private fun IncidentPosition.toCursor(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString("$createdAt|$id".toByteArray())

fun String.toIncidentPosition(): IncidentPosition = runCatching {
    val (createdAt, id) = String(Base64.getUrlDecoder().decode(this)).split('|', limit = 2)
    IncidentPosition(Instant.parse(createdAt), id)
}.getOrElse { throw InvalidIncidentException("cursor must be a nextCursor returned by this API") }

/** `all` searches every text field; any other value names the one field to search. */
fun String.toSearchField(): IncidentSearchField? =
    if (this == "all") null
    else IncidentSearchField.entries.firstOrNull { it.name.lowercase() == this }
        ?: throw InvalidIncidentException("field must be all, title, location, or description")
