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

    @Test
    fun `every problem code maps to its failure`() {
        assertEquals(IncidentFailure.INCIDENT_NOT_FOUND, failureForApiError("RESOURCE_NOT_FOUND", 404))
        assertEquals(IncidentFailure.MALFORMED_REQUEST, failureForApiError("INVALID_REQUEST", 400))
        // The code wins even when the status says otherwise.
        assertEquals(IncidentFailure.INCIDENT_VALIDATION_FAILED, failureForApiError("INVALID_INCIDENT", 500))
    }

    @Test
    fun `unknown codes fall back to the HTTP status`() {
        listOf(400, 405, 415, 422).forEach { status ->
            assertEquals(IncidentFailure.MALFORMED_REQUEST, failureForApiError("SOMETHING_NEW", status))
        }
        assertEquals(IncidentFailure.UPLOAD_TOO_LARGE, failureForApiError(null, 413))
        assertEquals(IncidentFailure.SERVER_ERROR, failureForApiError(null, 500))
    }

    @Test
    fun `a code that is not a string is ignored`() {
        val body = """{"code":404}""".toResponseBody("application/problem+json".toMediaType())

        assertEquals(IncidentFailure.MALFORMED_REQUEST, HttpException(Response.error<Any>(422, body)).toIncidentFailure())
    }

    @Test
    fun `a request returns its result and translates HTTP errors`() = runTest {
        assertEquals("ok", incidentRequest { "ok" })

        val failure = runCatching { incidentRequest<Unit> { throw error(404, "INCIDENT_NOT_FOUND") } }.exceptionOrNull()
        assertEquals(IncidentFailure.INCIDENT_NOT_FOUND, (failure as IncidentException).failure)
    }

    @Test
    fun `other exceptions are not translated`() = runTest {
        val bug = IllegalStateException("bug")

        assertEquals(bug, runCatching { incidentRequest<Unit> { throw bug } }.exceptionOrNull())
    }

    private suspend fun failureOf(error: IOException): IncidentFailure =
        (runCatching { incidentRequest<Unit> { throw error } }.exceptionOrNull() as IncidentException).failure

    private fun error(status: Int, code: String?): HttpException {
        val json = code?.let { """{"code":"$it"}""" } ?: "not json"
        return HttpException(Response.error<Any>(status, json.toResponseBody("application/problem+json".toMediaType())))
    }
}
