package com.hyejeanmoon.fieldincidentreporter.domain.model

// The server's limits. Text limits apply to the trimmed length, in characters.
const val MAX_INCIDENT_TITLE_LENGTH = 200
const val MAX_INCIDENT_LOCATION_LENGTH = 200
const val MAX_INCIDENT_DESCRIPTION_LENGTH = 4000
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

    /** The fields the server would reject, in form order. */
    fun missingFields(): List<RequiredIncidentField> = buildList {
        if (!title.fits(MAX_INCIDENT_TITLE_LENGTH)) add(RequiredIncidentField.TITLE)
        if (!location.fits(MAX_INCIDENT_LOCATION_LENGTH)) add(RequiredIncidentField.LOCATION)
        if (!description.fits(MAX_INCIDENT_DESCRIPTION_LENGTH)) add(RequiredIncidentField.DESCRIPTION)
        if (photos.size !in 1..MAX_INCIDENT_PHOTOS) add(RequiredIncidentField.PHOTOS)
    }
}

enum class RequiredIncidentField { TITLE, LOCATION, DESCRIPTION, PHOTOS }

/** Like the server, checks the trimmed text: it must not be blank or longer than [maxLength]. */
private fun String.fits(maxLength: Int) = trim().length in 1..maxLength
