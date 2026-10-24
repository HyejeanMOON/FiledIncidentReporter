package com.example.incidentreporter

import com.example.incidentreporter.incident.api.IncidentExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest

class IncidentExceptionHandlerTests {
    @Test
    fun `unexpected errors hide internal details`() {
        val request = MockHttpServletRequest("GET", "/incidents")
        val response = IncidentExceptionHandler().unexpected(IllegalStateException("database password is secret"), request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_ERROR", response.body?.properties?.get("code"))
        assertEquals("An unexpected server error occurred", response.body?.detail)
    }
}
