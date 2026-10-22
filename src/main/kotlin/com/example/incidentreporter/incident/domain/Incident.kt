package com.example.incidentreporter.incident.domain

import java.time.Instant

data class IncidentPhoto(val id: String, val uri: String)

data class Incident(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photos: List<IncidentPhoto>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resolvedAt: Instant? = null,
)

data class IncidentInput(
    val title: String,
    val location: String,
    val description: String,
) {
    fun validated(): IncidentInput {
        val normalized = copy(
            title = title.trim(),
            location = location.trim(),
            description = description.trim(),
        )
        if (normalized.title.isEmpty() || normalized.title.length > 200) {
            throw InvalidIncidentException("title must contain 1 to 200 characters")
        }
        if (normalized.location.isEmpty() || normalized.location.length > 200) {
            throw InvalidIncidentException("location must contain 1 to 200 characters")
        }
        if (normalized.description.isEmpty() || normalized.description.length > 4000) {
            throw InvalidIncidentException("description must contain 1 to 4000 characters")
        }
        return normalized
    }
}

class InvalidIncidentException(message: String) : RuntimeException(message)
class IncidentNotFoundException : RuntimeException("Incident not found")
class PhotoNotFoundException : RuntimeException("Photo not found")
