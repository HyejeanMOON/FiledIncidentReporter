package com.hyejeanmoon.fieldincidentreporter.domain.model

sealed interface IncidentPhoto {
    val displayUri: String
    val isBlank: Boolean

    /** A photo picked or captured on the device that has not been uploaded yet. */
    data class Local(val uri: String) : IncidentPhoto {
        override val displayUri: String get() = uri
        override val isBlank: Boolean get() = uri.isBlank()
    }

    /** A photo already stored on the server. */
    data class Stored(
        val id: String,
        override val displayUri: String,
    ) : IncidentPhoto {
        override val isBlank: Boolean get() = id.isBlank()
    }
}
