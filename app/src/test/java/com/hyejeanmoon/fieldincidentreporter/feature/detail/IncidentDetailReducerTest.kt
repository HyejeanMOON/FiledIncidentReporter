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

        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(failed, IncidentDetailUiEvent.Retry))
        assertEquals(loaded, reducer.reduce(loaded, IncidentDetailUiEvent.Retry))
    }

    @Test
    fun `reload keeps the report on screen and replaces it when loaded`() {
        val reloading = reducer.reduce(loaded.copy(error = IncidentFailure.SERVER_ERROR), IncidentDetailUiEvent.Reload)
        assertEquals(loaded.copy(isReloading = true), reloading)
        assertEquals(reloading, reducer.reduce(reloading, IncidentDetailUiEvent.Reload))
        assertEquals(reloading, reducer.reduce(reloading, IncidentDetailUiEvent.Delete))

        val edited = testIncident(title = "Edited")
        assertEquals(IncidentDetailUiState.Loaded(edited), reducer.reduce(reloading, IncidentDetailResult.Loaded(edited)))
    }

    @Test
    fun `a failed reload keeps the report and shows the error`() {
        val reloading = reducer.reduce(loaded, IncidentDetailUiEvent.Reload)

        assertEquals(
            loaded.copy(error = IncidentFailure.NETWORK_UNAVAILABLE),
            reducer.reduce(reloading, IncidentDetailResult.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE)),
        )
    }

    @Test
    fun `reload waits for a delete and needs a loaded report`() {
        val deleting = reducer.reduce(loaded, IncidentDetailUiEvent.Delete)

        assertEquals(deleting, reducer.reduce(deleting, IncidentDetailUiEvent.Reload))
        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailUiEvent.Reload))
    }

    @Test
    fun `delete starts only once and only for a loaded report`() {
        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailUiEvent.Delete))

        val deleting = reducer.reduce(loaded, IncidentDetailUiEvent.Delete)
        assertEquals(loaded.copy(isDeleting = true), deleting)
        assertEquals(deleting, reducer.reduce(deleting, IncidentDetailUiEvent.Delete))
    }

    @Test
    fun `a failed delete shows the error and re-enables the controls`() {
        val deleting = reducer.reduce(loaded, IncidentDetailUiEvent.Delete)

        val failed = reducer.reduce(deleting, IncidentDetailResult.DeleteFailed(IncidentFailure.SERVER_ERROR))
        assertEquals(loaded.copy(error = IncidentFailure.SERVER_ERROR), failed)
        assertEquals(loaded, reducer.reduce(failed, IncidentDetailUiEvent.ErrorDismissed))
    }

    @Test
    fun `a loaded report replaces the loading state`() {
        assertEquals(loaded, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailResult.Loaded(testIncident())))
    }

    @Test
    fun `a finished delete keeps the controls disabled`() {
        val deleting = reducer.reduce(loaded, IncidentDetailUiEvent.Delete)

        assertEquals(
            loaded.copy(isDeleting = true, isDeleted = true),
            reducer.reduce(deleting, IncidentDetailResult.Deleted),
        )
    }

    @Test
    fun `delete clears an error left from the last attempt`() {
        val failed = loaded.copy(error = IncidentFailure.SERVER_ERROR)

        assertEquals(loaded.copy(isDeleting = true), reducer.reduce(failed, IncidentDetailUiEvent.Delete))
    }

    @Test
    fun `actions and delete results need a loaded report`() {
        val failed = IncidentDetailUiState.LoadFailed(IncidentFailure.INCIDENT_NOT_FOUND)

        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailUiEvent.Retry))
        assertEquals(failed, reducer.reduce(failed, IncidentDetailUiEvent.Delete))
        assertEquals(failed, reducer.reduce(failed, IncidentDetailUiEvent.ErrorDismissed))
        assertEquals(IncidentDetailUiState.Loading, reducer.reduce(IncidentDetailUiState.Loading, IncidentDetailResult.Deleted))
        assertEquals(
            failed,
            reducer.reduce(failed, IncidentDetailResult.DeleteFailed(IncidentFailure.SERVER_ERROR)),
        )
    }
}
