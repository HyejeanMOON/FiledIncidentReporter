package com.hyejeanmoon.fieldincidentreporter.domain.model

sealed interface IncidentPhoto {
    /** The full-size image, for the photo viewer and the detail gallery. */
    val displayUri: String

    /** A small preview for lists and thumbnails. */
    val thumbnailUri: String

    /** A photo picked or captured on the device that has not been uploaded yet. */
    data class Local(val uri: String) : IncidentPhoto {
        override val displayUri: String get() = uri
        override val thumbnailUri: String get() = uri
    }

    /** A photo already stored on the server, with a server-made thumbnail. */
    data class Stored(
        val id: String,
        override val displayUri: String,
        override val thumbnailUri: String = displayUri,
    ) : IncidentPhoto
}
