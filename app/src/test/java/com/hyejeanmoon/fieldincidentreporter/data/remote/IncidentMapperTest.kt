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
    fun `list photos map to previews while detail photo paths remain separate`() {
        val page = gson.fromJson(
            """{"items":[{"id":"1","title":"Leak","location":"Bay","description":"Pipe","photos":["data:image/jpeg;base64,AAAA"]}],"page":0,"size":20,"hasNext":false}""",
            IncidentPageDto::class.java,
        )
        val summary = page.items.single().toSummary()
        assertEquals(listOf("data:image/jpeg;base64,AAAA"), summary.photoPreviews)

        val detail = gson.fromJson(
            """{"id":"1","title":"Leak","location":"Bay","description":"Pipe","photoUris":["/photos/1.jpg"]}""",
            IncidentDto::class.java,
        ).toIncident(photoUrlResolver)
        assertEquals(
            listOf(IncidentPhoto.Stored("/photos/1.jpg", "http://example.test/photos/1.jpg")),
            detail.photos,
        )
    }
}
