package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.application.IncidentService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/incidents")
class IncidentController(private val service: IncidentService) {
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) resolved: Boolean?,
    ): IncidentPageResponse = service.list(page, size, q, resolved).toResponse()

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): IncidentResponse = service.get(id).toResponse()

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestPart("incident") body: IncidentBody,
        @RequestPart("photos") photos: List<MultipartFile>,
    ): IncidentResponse = service.create(
        body.toInput(),
        photos.map(MultipartFile::toIncomingPhoto),
    ).toResponse()

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

    @PatchMapping("/{id}/resolution", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun setResolved(@PathVariable id: String, @RequestBody body: IncidentResolutionBody): IncidentResponse =
        service.setResolved(id, body.resolved).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String) = service.delete(id)
}
