package com.hyejeanmoon.fieldincidentreporter.domain.model

const val MAX_INCIDENT_PHOTOS = 20

data class IncidentDraft(
    val title: String,
    val location: String,
    val description: String,
    val photos: List<IncidentPhoto>,
) {
    fun normalized(): IncidentDraft = copy(
        title = title.trim(),
        location = location.trim(),
        description = description.trim(),
    )
}

enum class RequiredIncidentField {
    TITLE,
    LOCATION,
    DESCRIPTION,
    PHOTOS,
}
