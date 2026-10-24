package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_PHOTOS
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateIncidentDraftUseCaseTest {
    private val validate = ValidateIncidentDraftUseCase()
    private val complete = IncidentDraft("Title", "Room", "Details", photos(1))

    @Test
    fun `a complete draft has no missing fields`() {
        assertEquals(emptyList<RequiredIncidentField>(), validate(complete))
    }

    @Test
    fun `blank text fields and no photos are all reported, in form order`() {
        assertEquals(
            listOf(
                RequiredIncidentField.TITLE,
                RequiredIncidentField.LOCATION,
                RequiredIncidentField.DESCRIPTION,
                RequiredIncidentField.PHOTOS,
            ),
            validate(IncidentDraft(" ", "", "\n\t", emptyList())),
        )
    }

    @Test
    fun `a draft may have up to the maximum number of photos`() {
        assertEquals(emptyList<RequiredIncidentField>(), validate(complete.copy(photos = photos(MAX_INCIDENT_PHOTOS))))
        assertEquals(
            listOf(RequiredIncidentField.PHOTOS),
            validate(complete.copy(photos = photos(MAX_INCIDENT_PHOTOS + 1))),
        )
    }

    private fun photos(count: Int) = List(count) { IncidentPhoto.Local("content://photos/$it") }
}
