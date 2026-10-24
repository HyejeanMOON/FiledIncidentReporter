package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentDetailReducerTest {
    private val reducer = IncidentDetailReducer()
    private val loaded = reducer.reduce(IncidentDetailUiState(), IncidentDetailResult.Loaded(testIncident()))

    @Test
    fun `retry restarts loading only after a load failure`() {
        val failed = reducer.reduce(
            IncidentDetailUiState(),
            IncidentDetailResult.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE),
        )

        val retried = reducer.reduce(failed, IncidentDetailAction.Retry)
        assertEquals(true, retried.isLoading)
        assertEquals(null, retried.loadFailure)
        assertEquals(loaded, reducer.reduce(loaded, IncidentDetailAction.Retry))
    }

    @Test
    fun `delete starts only once and only for a loaded report`() {
        assertEquals(false, reducer.reduce(IncidentDetailUiState(), IncidentDetailAction.Delete).isDeleting)

        val deleting = reducer.reduce(loaded, IncidentDetailAction.Delete)
        assertEquals(true, deleting.isDeleting)
        assertEquals(deleting, reducer.reduce(deleting, IncidentDetailAction.Delete))
    }

    @Test
    fun `a failed delete shows the error and re-enables the controls`() {
        val deleting = reducer.reduce(loaded, IncidentDetailAction.Delete)

        val failed = reducer.reduce(deleting, IncidentDetailResult.DeleteFailed(IncidentFailure.SERVER_ERROR))
        assertEquals(false, failed.isDeleting)
        assertEquals(IncidentFailure.SERVER_ERROR, failed.error)
        assertEquals(null, reducer.reduce(failed, IncidentDetailAction.ErrorDismissed).error)
    }
}
