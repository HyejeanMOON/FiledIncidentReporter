package com.hyejeanmoon.fieldincidentreporter.domain.error

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class IncidentFailureTest {
    @Test
    fun `only an incident exception carries a specific failure`() {
        assertEquals(
            IncidentFailure.SERVER_ERROR,
            IncidentException(IncidentFailure.SERVER_ERROR, IOException()).toIncidentFailure(),
        )
        assertEquals(IncidentFailure.UNEXPECTED, IllegalStateException().toIncidentFailure())
    }

    @Test
    fun `suspend catching wraps the result and exceptions`() = runTest {
        val error = IllegalStateException("boom")

        assertEquals(Result.success(1), runSuspendCatching { 1 })
        assertEquals(error, runSuspendCatching { throw error }.exceptionOrNull())
    }

    @Test
    fun `suspend catching lets cancellation propagate`() = runTest {
        val outcome = runCatching { runSuspendCatching { throw CancellationException("cancelled") } }

        assertEquals(true, outcome.exceptionOrNull() is CancellationException)
    }
}
