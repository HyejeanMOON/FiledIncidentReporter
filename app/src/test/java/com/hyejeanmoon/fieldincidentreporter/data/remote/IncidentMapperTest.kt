package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.google.gson.Gson
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class IncidentMapperTest {
    private val gson = Gson()
    private val baseUrl = "http://example.test/api/".toHttpUrl()

    @Test
    fun `list and detail photos resolve the full image URL against the api origin`() {
        val page = gson.fromJson(
            """{"items":[$IncidentJson],"page":0,"size":20,"hasNext":false}""",
            IncidentPageDto::class.java,
        ).toIncidentPage(baseUrl)
        val detail = gson.fromJson(IncidentJson, IncidentDto::class.java).toIncident(baseUrl)

        val expectedPhoto = IncidentPhoto.Stored("photo-1", "http://example.test/incidents/1/photos/photo-1")
        assertEquals(listOf(expectedPhoto), page.items.single().photos)
        assertEquals(page.items.single(), detail)
        assertEquals(Instant.parse("2026-10-20T09:00:00Z"), detail.createdAt)
        assertEquals(true, detail.isEdited)
    }

    private companion object {
        const val IncidentJson =
            """{"id":"1","title":"Leak","location":"Bay","description":"Pipe","createdAt":"2026-10-20T09:00:00Z","updatedAt":"2026-10-21T11:30:00Z","photos":[{"id":"photo-1","url":"/incidents/1/photos/photo-1","thumbnailUrl":"/incidents/1/photos/photo-1/thumbnail"}]}"""
    }
}
