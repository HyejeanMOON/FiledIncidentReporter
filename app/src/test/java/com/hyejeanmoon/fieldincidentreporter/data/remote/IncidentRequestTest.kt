package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentException
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class IncidentRequestTest {
    @Test
    fun `problem code takes precedence over the status fallback`() {
        assertEquals(IncidentFailure.INCIDENT_VALIDATION_FAILED, error(400, "INVALID_INCIDENT").toIncidentFailure())
        assertEquals(IncidentFailure.UPLOAD_TOO_LARGE, error(413, "PAYLOAD_TOO_LARGE").toIncidentFailure())
        assertEquals(IncidentFailure.INCIDENT_NOT_FOUND, error(404, "INCIDENT_NOT_FOUND").toIncidentFailure())
        assertEquals(IncidentFailure.SERVER_ERROR, error(500, "INTERNAL_ERROR").toIncidentFailure())
    }

    @Test
    fun `missing or malformed problem body falls back to HTTP status`() {
        assertEquals(IncidentFailure.MALFORMED_REQUEST, error(400, null).toIncidentFailure())
        assertEquals(IncidentFailure.INCIDENT_NOT_FOUND, error(404, null).toIncidentFailure())
        assertEquals(IncidentFailure.SERVER_ERROR, error(503, null).toIncidentFailure())
    }

    @Test
    fun `photo errors are told apart from network errors`() = runTest {
        assertEquals(IncidentFailure.PHOTO_FORMAT_UNSUPPORTED, failureOf(UnsupportedPhotoFormatException("bmp")))
        assertEquals(IncidentFailure.PHOTO_UNREADABLE, failureOf(PhotoReadException("gone")))
        assertEquals(IncidentFailure.NETWORK_UNAVAILABLE, failureOf(IOException("offline")))
    }

    private suspend fun failureOf(error: IOException): IncidentFailure =
        (runCatching { incidentRequest<Unit> { throw error } }.exceptionOrNull() as IncidentException).failure

    private fun error(status: Int, code: String?): HttpException {
        val json = code?.let { """{"code":"$it"}""" } ?: "not json"
        return HttpException(Response.error<Any>(status, json.toResponseBody("application/problem+json".toMediaType())))
    }
}
