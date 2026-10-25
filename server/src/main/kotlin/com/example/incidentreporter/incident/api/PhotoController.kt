package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.application.IncidentService
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/incidents/{incidentId}/photos/{photoId}")
class PhotoController(
    private val service: IncidentService,
    private val previewEncoder: PhotoPreviewEncoder,
) {
    @GetMapping
    fun get(@PathVariable incidentId: String, @PathVariable photoId: String): ResponseEntity<Resource> {
        val photo = service.getPhoto(incidentId, photoId)
        return photoResponse(photo.contentType)
            .contentLength(photo.size)
            .body(InputStreamResource(photo.openStream()))
    }

    @GetMapping("/thumbnail")
    fun thumbnail(@PathVariable incidentId: String, @PathVariable photoId: String): ResponseEntity<ByteArray> {
        val preview = previewEncoder.encode(service.getPhoto(incidentId, photoId))
        return photoResponse(preview.contentType).body(preview.bytes)
    }

    private fun photoResponse(contentType: String) = ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        // Photo IDs are never reused and stored photos never change, so browsers may keep them.
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
        .header("X-Content-Type-Options", "nosniff")
}
