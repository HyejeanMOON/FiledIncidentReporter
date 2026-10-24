package com.hyejeanmoon.fieldincidentreporter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentDraftTest {
    private val complete = IncidentDraft("Title", "Room", "Details", photos(1))

    @Test
    fun `a complete draft has no missing fields`() {
        assertEquals(emptyList<RequiredIncidentField>(), complete.missingFields())
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
            IncidentDraft(" ", "", "\n\t", emptyList()).missingFields(),
        )
    }

    @Test
    fun `text is checked against the server's limits after trimming`() {
        val atLimit = complete.copy(title = " ${"t".repeat(200)} ", location = "l".repeat(200), description = "d".repeat(4000))
        assertEquals(emptyList<RequiredIncidentField>(), atLimit.missingFields())

        val overLimit = atLimit.copy(title = "t".repeat(201), location = "l".repeat(201), description = "d".repeat(4001))
        assertEquals(
            listOf(RequiredIncidentField.TITLE, RequiredIncidentField.LOCATION, RequiredIncidentField.DESCRIPTION),
            overLimit.missingFields(),
        )
    }

    @Test
    fun `a draft may have up to the maximum number of photos`() {
        assertEquals(emptyList<RequiredIncidentField>(), complete.copy(photos = photos(MAX_INCIDENT_PHOTOS)).missingFields())
        assertEquals(
            listOf(RequiredIncidentField.PHOTOS),
            complete.copy(photos = photos(MAX_INCIDENT_PHOTOS + 1)).missingFields(),
        )
    }

    private fun photos(count: Int) = List(count) { IncidentPhoto.Local("content://photos/$it") }
}
