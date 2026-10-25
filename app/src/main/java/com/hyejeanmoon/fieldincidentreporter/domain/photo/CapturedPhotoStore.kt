package com.hyejeanmoon.fieldincidentreporter.domain.photo

/** Files the camera writes photos into before they are uploaded. */
interface CapturedPhotoStore {
    /** A new, empty file for the camera to write to, as a content URI string. */
    fun newPhotoUri(): String

    /** Deletes every captured photo. */
    fun clear()
}
