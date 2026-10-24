package com.hyejeanmoon.fieldincidentreporter.testing

import com.hyejeanmoon.fieldincidentreporter.domain.repository.CapturedPhotoStore

class FakeCapturedPhotoStore : CapturedPhotoStore {
    val photos = mutableListOf<String>()

    override fun newPhotoUri(): String = "content://captured/${photos.size}".also(photos::add)

    override fun clear() = photos.clear()
}
