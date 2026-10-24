package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.di.ApiJson
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class IncidentMapperTest {
    private val baseUrl = "http://example.test/api/".toHttpUrl()

    @Test
    fun `list and detail photos resolve the full image URL against the api origin`() {
        val listItem = ApiJson.decodeFromString<IncidentPageDto>(
            """{"items":[$IncidentJson],"nextCursor":null}""",
        ).items.single().toIncident(baseUrl)
        val detail = ApiJson.decodeFromString<IncidentDto>(IncidentJson).toIncident(baseUrl)

        val expectedPhoto = IncidentPhoto.Stored("photo-1", "http://example.test/incidents/1/photos/photo-1")
        assertEquals(listOf(expectedPhoto), listItem.photos)
        assertEquals(listItem, detail)
        assertEquals(Instant.parse("2026-10-20T09:00:00Z"), detail.createdAt)
        assertEquals(true, detail.isEdited)
    }

    @Test
    fun `missing or null photos and unknown fields are tolerated`() {
        val base = """"id":"1","title":"Leak","location":"Bay","description":"Pipe","createdAt":"2026-10-20T09:00:00Z","updatedAt":"2026-10-20T09:00:00Z""""
        assertEquals(emptyList<IncidentPhoto>(), ApiJson.decodeFromString<IncidentDto>("{$base}").toIncident(baseUrl).photos)
        assertEquals(
            emptyList<IncidentPhoto>(),
            ApiJson.decodeFromString<IncidentDto>("""{$base,"photos":null,"extra":1}""").toIncident(baseUrl).photos,
        )
    }

    @Test
    fun `photos without an id are dropped`() {
        val dto = ApiJson.decodeFromString<IncidentDto>(IncidentJson)
        val blank = dto.photos.single().copy(id = " ")

        val photos = dto.copy(photos = dto.photos + blank).toIncident(baseUrl).photos
        assertEquals(listOf("photo-1"), photos.map { (it as IncidentPhoto.Stored).id })
    }

    @Test
    fun `absolute and unresolvable photo URLs are kept as they are`() {
        assertEquals("https://cdn.example.test/photo-1", baseUrl.resolveOrSelf("https://cdn.example.test/photo-1"))
        assertEquals("http://", baseUrl.resolveOrSelf("http://"))
    }

    @Test
    fun `a report is edited only when it was updated after it was created`() {
        val incident = ApiJson.decodeFromString<IncidentDto>(IncidentJson).toIncident(baseUrl)

        assertEquals(false, incident.copy(updatedAt = incident.createdAt).isEdited)
    }

    private companion object {
        const val IncidentJson =
            """{"id":"1","title":"Leak","location":"Bay","description":"Pipe","createdAt":"2026-10-20T09:00:00Z","updatedAt":"2026-10-21T11:30:00Z","photos":[{"id":"photo-1","url":"/incidents/1/photos/photo-1","thumbnailUrl":"/incidents/1/photos/photo-1/thumbnail"}]}"""
    }
}
