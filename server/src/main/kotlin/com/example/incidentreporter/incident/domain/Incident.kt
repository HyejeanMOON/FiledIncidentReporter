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
) {
    companion object {
        const val MAX_PHOTOS = 20

        fun requireValidPhotoCount(count: Int) {
            if (count !in 1..MAX_PHOTOS) throw InvalidIncidentException("An incident needs 1 to $MAX_PHOTOS photos")
        }
    }
}

data class IncidentInput(
    val title: String,
    val location: String,
    val description: String,
) {
    fun validated() = IncidentInput(
        title = title.requireLength("title", 200),
        location = location.requireLength("location", 200),
        description = description.requireLength("description", 4000),
    )

    private fun String.requireLength(field: String, max: Int): String = trim().also {
        if (it.isEmpty() || it.length > max) throw InvalidIncidentException("$field must contain 1 to $max characters")
    }
}
