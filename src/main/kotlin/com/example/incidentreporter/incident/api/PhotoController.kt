package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.domain.PhotoStore
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PhotoController(private val photoStore: PhotoStore) {
    @GetMapping("/photos/{filename}")
    fun get(@PathVariable filename: String): ResponseEntity<Resource> {
        val photo = photoStore.load("/photos/$filename") ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .contentLength(photo.size)
            .header("X-Content-Type-Options", "nosniff")
            .body(InputStreamResource(photo.openStream()))
    }
}
