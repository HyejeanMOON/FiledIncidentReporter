package com.example.incidentreporter.incident.domain

data class Incident(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
)

data class IncidentInput(
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
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
        if (photoUris.isEmpty() || photoUris.size > 20 || photoUris.any { it.isBlank() || it.length > 2048 }) {
            throw InvalidIncidentException("photoUris must contain 1 to 20 non-blank values of at most 2048 characters")
        }
        return normalized
    }
}

class InvalidIncidentException(message: String) : RuntimeException(message)

class IncidentNotFoundException : RuntimeException("Incident not found")
