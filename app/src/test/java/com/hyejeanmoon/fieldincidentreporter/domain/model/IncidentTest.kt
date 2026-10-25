package com.hyejeanmoon.fieldincidentreporter.domain.model

import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import com.hyejeanmoon.fieldincidentreporter.testing.testTime
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentTest {
    private val created = testIncident()
    private val later = testTime.plusSeconds(60)

    @Test
    fun `a new report is not updated`() {
        assertEquals(false, created.isUpdated)
    }

    @Test
    fun `an edit or a resolution updates the report`() {
        assertEquals(true, created.copy(updatedAt = later).isUpdated)
        assertEquals(true, created.copy(updatedAt = later, resolvedAt = later).isUpdated)
    }
}
