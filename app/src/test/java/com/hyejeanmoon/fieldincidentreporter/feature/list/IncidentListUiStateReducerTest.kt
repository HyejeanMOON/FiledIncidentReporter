package com.hyejeanmoon.fieldincidentreporter.feature.list

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentListUiStateReducerTest {
    @Test
    fun `lookup error dialog can be dismissed without losing retry state`() {
        val started = IncidentListUiStateReducer.reduce(IncidentListUiState(), IncidentListStateChange.LookupStarted("incident-1"))
        val failed = IncidentListUiStateReducer.reduce(
            started,
            IncidentListStateChange.LookupFailed("incident-1", IncidentFailure.SERVER),
        )
        assertTrue(failed.lookupDialogVisible)

        val dismissed = IncidentListUiStateReducer.reduce(failed, IncidentListStateChange.LookupErrorDismissed)
        assertFalse(dismissed.lookupDialogVisible)
        assertEquals(IncidentFailure.SERVER, dismissed.lookupFailure)

        val retried = IncidentListUiStateReducer.reduce(dismissed, IncidentListStateChange.LookupStarted("incident-1"))
        assertEquals(null, retried.lookupFailure)
        assertFalse(retried.lookupDialogVisible)
    }
}
