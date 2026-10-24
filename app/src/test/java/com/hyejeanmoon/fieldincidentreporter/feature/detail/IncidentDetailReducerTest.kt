package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentDetailReducerTest {
    private val reducer = IncidentDetailReducer()
    private val loaded = IncidentDetailUiState.Loaded(testIncident())

    @Test
    fun `retry restarts loading only after a load failure`() {
        val failed = reducer.reduce(
            IncidentDetailUiState.Loading,
            IncidentDetailResult.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE),
        )

        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(failed, IncidentDetailAction.Retry))
        assertEquals(loaded, reducer.reduce(loaded, IncidentDetailAction.Retry))
    }

    @Test
    fun `delete starts only once and only for a loaded report`() {
        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailAction.Delete))

        val deleting = reducer.reduce(loaded, IncidentDetailAction.Delete)
        assertEquals(loaded.copy(isDeleting = true), deleting)
        assertEquals(deleting, reducer.reduce(deleting, IncidentDetailAction.Delete))
    }

    @Test
    fun `a failed delete shows the error and re-enables the controls`() {
        val deleting = reducer.reduce(loaded, IncidentDetailAction.Delete)

        val failed = reducer.reduce(deleting, IncidentDetailResult.DeleteFailed(IncidentFailure.SERVER_ERROR))
        assertEquals(loaded.copy(error = IncidentFailure.SERVER_ERROR), failed)
        assertEquals(loaded, reducer.reduce(failed, IncidentDetailAction.ErrorDismissed))
    }

    @Test
    fun `a loaded report replaces the loading state`() {
        assertEquals(loaded, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailResult.Loaded(testIncident())))
    }

    @Test
    fun `a finished delete keeps the controls disabled`() {
        val deleting = reducer.reduce(loaded, IncidentDetailAction.Delete)

        assertEquals(
            loaded.copy(isDeleting = true, isDeleted = true),
            reducer.reduce(deleting, IncidentDetailResult.Deleted),
        )
    }

    @Test
    fun `delete clears an error left from the last attempt`() {
        val failed = loaded.copy(error = IncidentFailure.SERVER_ERROR)

        assertEquals(loaded.copy(isDeleting = true), reducer.reduce(failed, IncidentDetailAction.Delete))
    }

    @Test
    fun `actions and delete results need a loaded report`() {
        val failed = IncidentDetailUiState.LoadFailed(IncidentFailure.INCIDENT_NOT_FOUND)

        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailAction.Retry))
        assertEquals(failed, reducer.reduce(failed, IncidentDetailAction.Delete))
        assertEquals(failed, reducer.reduce(failed, IncidentDetailAction.ErrorDismissed))
        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailResult.Deleted))
        assertEquals(
            failed,
            reducer.reduce(failed, IncidentDetailResult.DeleteFailed(IncidentFailure.SERVER_ERROR)),
        )
    }
}
