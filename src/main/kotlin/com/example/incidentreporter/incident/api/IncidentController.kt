package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.application.IncidentService
import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentPhoto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import com.example.incidentreporter.incident.domain.IncomingPhoto
import java.time.Instant

data class IncidentBody(
    val title: String,
    val location: String,
    val description: String,
) {
    fun toInput() = IncidentInput(title, location, description)
}

data class IncidentUpdateBody(
    val title: String,
    val location: String,
    val description: String,
    val deletePhotoIds: List<String> = emptyList(),
) {
    fun toInput() = IncidentInput(title, location, description)
}

private fun MultipartFile.toIncomingPhoto() = IncomingPhoto(contentType, size, this::getInputStream)

data class PhotoResponse(val id: String, val url: String, val thumbnailUrl: String)

private fun IncidentPhoto.toResponse(incidentId: String): PhotoResponse {
    val url = "/incidents/$incidentId/photos/$id"
    return PhotoResponse(id, url, "$url/thumbnail")
}

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

fun Incident.toResponse() = IncidentResponse(
    id, title, location, description, photos.map { it.toResponse(id) }, createdAt, updatedAt,
    resolvedAt != null, resolvedAt,
)

data class IncidentResolutionBody(val resolved: Boolean)

data class IncidentPageResponse(
    val items: List<IncidentResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

@RestController
@RequestMapping("/incidents")
class IncidentController(private val service: IncidentService) {
    @GetMapping("/{id}")
    fun get(@PathVariable id: String): IncidentResponse = service.get(id).toResponse()

    @PatchMapping("/{id}/resolution", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun setResolved(@PathVariable id: String, @RequestBody body: IncidentResolutionBody): IncidentResponse =
        service.setResolved(id, body.resolved).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String) = service.delete(id)

    @PatchMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable id: String,
        @RequestPart("incident") body: IncidentUpdateBody,
        @RequestPart("photos", required = false) photos: List<MultipartFile>?,
    ): IncidentResponse = service.update(
        id,
        body.toInput(),
        body.deletePhotoIds,
        photos.orEmpty().map(MultipartFile::toIncomingPhoto),
    ).toResponse()

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) resolved: Boolean?,
    ): IncidentPageResponse {
        val result = service.list(page, size, q, resolved)
        return IncidentPageResponse(
            result.items.map(Incident::toResponse),
            result.page,
            result.size,
            result.hasNext,
        )
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestPart("incident") body: IncidentBody,
        @RequestPart("photos") photos: List<MultipartFile>,
    ): IncidentResponse = service.create(
        body.toInput(),
        photos.map(MultipartFile::toIncomingPhoto),
    ).toResponse()
}
