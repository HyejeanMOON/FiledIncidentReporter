package com.hyejeanmoon.fieldincidentreporter.domain.model

enum class RequiredIncidentField {
    TITLE,
    LOCATION,
    DESCRIPTION,
    PHOTOS,
}

internal fun missingRequiredFields(
    title: String,
    location: String,
    description: String,
    photoUris: List<String>,
): List<RequiredIncidentField> = buildList {
    if (title.isBlank()) add(RequiredIncidentField.TITLE)
    if (location.isBlank()) add(RequiredIncidentField.LOCATION)
    if (description.isBlank()) add(RequiredIncidentField.DESCRIPTION)
    if (photoUris.none { it.isNotBlank() }) add(RequiredIncidentField.PHOTOS)
}
