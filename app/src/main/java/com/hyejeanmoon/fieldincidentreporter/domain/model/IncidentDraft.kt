package com.hyejeanmoon.fieldincidentreporter.domain.model

data class IncidentDraft(
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
) {
    fun normalized(): IncidentDraft = copy(
        title = title.trim(),
        location = location.trim(),
        description = description.trim(),
        photoUris = photoUris.filter { it.isNotBlank() },
    )
}
