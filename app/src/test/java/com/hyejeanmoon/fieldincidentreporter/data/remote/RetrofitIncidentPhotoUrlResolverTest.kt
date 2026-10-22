package com.hyejeanmoon.fieldincidentreporter.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit

class RetrofitIncidentPhotoUrlResolverTest {
    @Test
    fun `server photo path resolves against configured api origin`() {
        val retrofit = Retrofit.Builder().baseUrl("http://example.test/api/").build()
        val resolver = RetrofitIncidentPhotoUrlResolver(retrofit)

        assertEquals("http://example.test/photos/1.jpg", resolver.resolve("/photos/1.jpg"))
    }
}
