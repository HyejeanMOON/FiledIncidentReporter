package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.application.IncidentService
import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import com.example.incidentreporter.incident.domain.IncomingPhoto

data class IncidentBody(
    val title: String,
    val location: String,
    val description: String,
) {
    fun toInput() = IncidentInput(title, location, description, emptyList())
}

data class IncidentUpdateBody(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
) {
    fun toInput() = IncidentInput(title, location, description, photoUris)
}

data class IncidentResponse(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
)

fun Incident.toResponse() = IncidentResponse(id, title, location, description, photoUris)

data class IncidentPageResponse(
    val items: List<IncidentResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

fun IncidentPage.toResponse() = IncidentPageResponse(items.map { it.toResponse() }, page, size, hasNext)

@RestController
@RequestMapping("/incidents")
class IncidentController(private val service: IncidentService) {
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String) = service.delete(id)

    @PutMapping("/{id}")
    fun update(@PathVariable id: String, @RequestBody body: IncidentUpdateBody): IncidentResponse {
        if (id != body.id) throw InvalidIncidentException("Body id must match path id")
        return service.update(id, body.toInput()).toResponse()
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): IncidentPageResponse = service.list(page, size).toResponse()

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestPart("incident") body: IncidentBody,
        @RequestPart("photos") photos: List<MultipartFile>,
    ): IncidentResponse = service.create(
        body.toInput(),
        photos.map { photo -> IncomingPhoto(photo.contentType, photo.size, photo::getInputStream) },
    ).toResponse()
}
