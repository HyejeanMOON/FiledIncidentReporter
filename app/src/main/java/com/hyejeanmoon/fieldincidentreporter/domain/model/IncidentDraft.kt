package com.hyejeanmoon.fieldincidentreporter.domain.model

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
        photos = photos.filter { photo ->
            when (photo) {
                is IncidentPhoto.Local -> photo.uri.isNotBlank()
                is IncidentPhoto.Stored -> photo.reference.isNotBlank()
            }
        },
    )
}
