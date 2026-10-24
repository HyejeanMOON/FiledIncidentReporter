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

/** [maxLength] is the server's limit on a text field's trimmed length, in characters. */
enum class RequiredIncidentField(val maxLength: Int? = null) {
    TITLE(maxLength = 200),
    LOCATION(maxLength = 200),
    DESCRIPTION(maxLength = 4000),
    PHOTOS,
}
