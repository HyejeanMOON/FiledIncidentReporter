package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class IncidentRequestTest {
    @Test
    fun `problem code takes precedence over the status fallback`() {
        assertEquals(IncidentFailure.INVALID_INCIDENT, error(400, "INVALID_INCIDENT").toIncidentFailure())
        assertEquals(IncidentFailure.PAYLOAD_TOO_LARGE, error(413, "PAYLOAD_TOO_LARGE").toIncidentFailure())
        assertEquals(IncidentFailure.NOT_FOUND, error(404, "INCIDENT_NOT_FOUND").toIncidentFailure())
        assertEquals(IncidentFailure.SERVER, error(500, "INTERNAL_ERROR").toIncidentFailure())
    }

    @Test
    fun `missing or malformed problem body falls back to HTTP status`() {
        assertEquals(IncidentFailure.INVALID_REQUEST, error(400, null).toIncidentFailure())
        assertEquals(IncidentFailure.NOT_FOUND, error(404, null).toIncidentFailure())
        assertEquals(IncidentFailure.SERVER, error(503, null).toIncidentFailure())
    }

    private fun error(status: Int, code: String?): HttpException {
        val json = code?.let { """{"code":"$it"}""" } ?: "not json"
        return HttpException(Response.error<Any>(status, json.toResponseBody("application/problem+json".toMediaType())))
    }
}
