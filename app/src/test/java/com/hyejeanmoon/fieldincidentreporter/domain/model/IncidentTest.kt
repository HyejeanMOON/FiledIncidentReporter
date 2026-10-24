package com.hyejeanmoon.fieldincidentreporter.domain.model

import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import com.hyejeanmoon.fieldincidentreporter.testing.testTime
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentTest {
    private val created = testIncident()
    private val later = testTime.plusSeconds(60)

    @Test
    fun `a new report has no separate update time`() {
        assertEquals(false, created.hasSeparateUpdateTime)
    }

    @Test
    fun `an edit has its own update time`() {
        assertEquals(true, created.copy(updatedAt = later).hasSeparateUpdateTime)
        assertEquals(true, created.copy(updatedAt = later.plusSeconds(60), resolvedAt = later).hasSeparateUpdateTime)
    }

    @Test
    fun `resolving a report does not count as an update`() {
        assertEquals(false, created.copy(updatedAt = later, resolvedAt = later).hasSeparateUpdateTime)
    }
}
