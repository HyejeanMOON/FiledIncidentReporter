package com.hyejeanmoon.fieldincidentreporter.domain.model

sealed interface IncidentPhoto {
    val displayUri: String

    data class Local(val uri: String) : IncidentPhoto {
        override val displayUri: String get() = uri
    }

    data class Stored(
        val reference: String,
        override val displayUri: String,
    ) : IncidentPhoto
}
