package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.application.IncidentService
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PhotoController(
    private val service: IncidentService,
    private val previewEncoder: PhotoPreviewEncoder,
) {
    @GetMapping("/incidents/{incidentId}/photos/{photoId}")
    fun get(@PathVariable incidentId: String, @PathVariable photoId: String): ResponseEntity<Resource> {
        val photo = service.getPhoto(incidentId, photoId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .contentLength(photo.size)
            .header("X-Content-Type-Options", "nosniff")
            .body(InputStreamResource(photo.openStream()))
    }

    @GetMapping("/incidents/{incidentId}/photos/{photoId}/thumbnail")
    fun thumbnail(@PathVariable incidentId: String, @PathVariable photoId: String): ResponseEntity<ByteArray> {
        val preview = previewEncoder.encode(service.getPhoto(incidentId, photoId))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(preview.contentType))
            .header("X-Content-Type-Options", "nosniff")
            .body(preview.bytes)
    }
}
