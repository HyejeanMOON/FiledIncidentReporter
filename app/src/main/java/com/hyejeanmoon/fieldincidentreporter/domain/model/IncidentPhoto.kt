package com.hyejeanmoon.fieldincidentreporter.domain.model

sealed interface IncidentPhoto {
    val displayUri: String

    /** A photo picked or captured on the device that has not been uploaded yet. */
    data class Local(val uri: String) : IncidentPhoto {
        override val displayUri: String get() = uri
    }

    /** A photo already stored on the server. */
    data class Stored(
        val id: String,
        override val displayUri: String,
    ) : IncidentPhoto
}
