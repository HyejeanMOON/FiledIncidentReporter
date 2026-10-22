package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.google.gson.Gson
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit

class IncidentMapperTest {
    private val gson = Gson()
    private val photoUrlResolver = RetrofitIncidentPhotoUrlResolver(
        Retrofit.Builder().baseUrl("http://example.test/").build(),
    )

    @Test
    fun `list previews and detail photos use the full image URL`() {
        val page = gson.fromJson(
            """{"items":[{"id":"1","title":"Leak","location":"Bay","description":"Pipe","photos":[{"id":"photo-1","url":"/incidents/1/photos/photo-1","thumbnailUrl":"/incidents/1/photos/photo-1/thumbnail"}]}],"page":0,"size":20,"hasNext":false}""",
            IncidentPageDto::class.java,
        )
        val summary = page.items.single().toSummary(photoUrlResolver)
        assertEquals(listOf("http://example.test/incidents/1/photos/photo-1"), summary.photoPreviews)

        val detail = gson.fromJson(
            """{"id":"1","title":"Leak","location":"Bay","description":"Pipe","photos":[{"id":"photo-1","url":"/incidents/1/photos/photo-1","thumbnailUrl":"/incidents/1/photos/photo-1/thumbnail"}]}""",
            IncidentDto::class.java,
        ).toIncident(photoUrlResolver)
        assertEquals(
            listOf(IncidentPhoto.Stored("photo-1", "http://example.test/incidents/1/photos/photo-1")),
            detail.photos,
        )
    }
}
