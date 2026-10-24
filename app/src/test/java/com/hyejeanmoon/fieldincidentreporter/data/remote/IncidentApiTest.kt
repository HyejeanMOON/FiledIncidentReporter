package com.hyejeanmoon.fieldincidentreporter.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit

/** Runs [IncidentApi] through real Retrofit against canned HTTP responses. */
class IncidentApiTest {
    @Test
    fun `delete accepts the server's empty 204 response`() = runTest {
        api(status = 204).delete("incident-1")
    }

    @Test
    fun `delete throws when the server does not confirm it`() = runTest {
        listOf(404, 500).forEach { status ->
            val error = runCatching { api(status).delete("incident-1") }.exceptionOrNull()

            assertEquals(status, (error as HttpException).code())
        }
    }

    private fun api(status: Int): IncidentApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("")
                    .body("".toResponseBody())
                    .build()
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("http://example.test/")
            .client(client)
            .build()
            .create(IncidentApi::class.java)
    }
}
