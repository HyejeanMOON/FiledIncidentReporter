package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.google.gson.Gson
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import retrofit2.Retrofit

class IncidentMapperTest {
    private val gson = Gson()
    private val photoUrlResolver = RetrofitIncidentPhotoUrlResolver(
        Retrofit.Builder().baseUrl("http://example.test/").build(),
    )

    @Test
    fun `list previews and detail photos use the full image URL`() {
        val page = gson.fromJson(
            """{"items":[{"id":"1","title":"Leak","location":"Bay","description":"Pipe","createdAt":"2026-10-20T09:00:00Z","updatedAt":"2026-10-21T11:30:00Z","photos":[{"id":"photo-1","url":"/incidents/1/photos/photo-1","thumbnailUrl":"/incidents/1/photos/photo-1/thumbnail"}]}],"page":0,"size":20,"hasNext":false}""",
            IncidentPageDto::class.java,
        )
        val summary = page.items.single().toSummary(photoUrlResolver)
        assertEquals(listOf("http://example.test/incidents/1/photos/photo-1"), summary.photoPreviews)
        assertEquals(Instant.parse("2026-10-20T09:00:00Z"), summary.createdAt)
        assertEquals(Instant.parse("2026-10-21T11:30:00Z"), summary.updatedAt)

        val detail = gson.fromJson(
            """{"id":"1","title":"Leak","location":"Bay","description":"Pipe","createdAt":"2026-10-20T09:00:00Z","updatedAt":"2026-10-21T11:30:00Z","photos":[{"id":"photo-1","url":"/incidents/1/photos/photo-1","thumbnailUrl":"/incidents/1/photos/photo-1/thumbnail"}]}""",
            IncidentDto::class.java,
        ).toIncident(photoUrlResolver)
        assertEquals(
            listOf(IncidentPhoto.Stored("photo-1", "http://example.test/incidents/1/photos/photo-1")),
            detail.photos,
        )
        assertEquals(summary.createdAt, detail.createdAt)
        assertEquals(summary.updatedAt, detail.updatedAt)
    }
}
