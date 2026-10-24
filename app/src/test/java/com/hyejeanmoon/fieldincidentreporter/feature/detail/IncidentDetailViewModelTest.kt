package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.lifecycle.SavedStateHandle
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentException
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.testing.MainDispatcherRule
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class IncidentDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeIncidentRepository(mutableMapOf("incident-1" to testIncident()))

    private fun viewModel() = IncidentDetailViewModel(
        SavedStateHandle(mapOf("incidentId" to "incident-1")),
        repository,
        IncidentDetailReducer(),
    )

    @Test
    fun `failed load can be retried`() {
        repository.failure = IncidentException(IncidentFailure.NETWORK_UNAVAILABLE, IOException())
        val viewModel = viewModel()
        assertEquals(IncidentDetailUiState.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE), viewModel.uiState.value)

        repository.failure = null
        viewModel.onAction(IncidentDetailAction.Retry)

        assertEquals(IncidentDetailUiState.Loaded(testIncident()), viewModel.uiState.value)
    }

    @Test
    fun `delete keeps controls disabled and reports completion`() {
        val viewModel = viewModel()
        viewModel.onAction(IncidentDetailAction.Delete)

        assertEquals(
            IncidentDetailUiState.Loaded(testIncident(), isDeleting = true, isDeleted = true),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `failed delete keeps the report and shows the error`() {
        val viewModel = viewModel()
        repository.failure = IncidentException(IncidentFailure.SERVER_ERROR, IOException())
        viewModel.onAction(IncidentDetailAction.Delete)

        assertEquals(
            IncidentDetailUiState.Loaded(testIncident(), error = IncidentFailure.SERVER_ERROR),
            viewModel.uiState.value,
        )

        repository.failure = null
        viewModel.onAction(IncidentDetailAction.ErrorDismissed)
        viewModel.onAction(IncidentDetailAction.Delete)
        assertEquals(true, (viewModel.uiState.value as IncidentDetailUiState.Loaded).isDeleted)
    }

    @Test
    fun `reload shows the report as saved by the edit`() {
        val viewModel = viewModel()
        repository.incidents["incident-1"] = testIncident(title = "Edited")

        viewModel.onAction(IncidentDetailAction.Reload)

        assertEquals(IncidentDetailUiState.Loaded(testIncident(title = "Edited")), viewModel.uiState.value)
    }

    @Test
    fun `an untranslated error is shown as unexpected`() {
        repository.failure = IllegalStateException("bug")

        assertEquals(IncidentDetailUiState.LoadFailed(IncidentFailure.UNEXPECTED), viewModel().uiState.value)
    }

    @Test
    fun `retry does not reload a report that is already loaded`() {
        val viewModel = viewModel()
        // Reloading would fail, so the loaded state staying shows that nothing was reloaded.
        repository.failure = IncidentException(IncidentFailure.NETWORK_UNAVAILABLE, IOException())
        viewModel.onAction(IncidentDetailAction.Retry)

        assertEquals(IncidentDetailUiState.Loaded(testIncident()), viewModel.uiState.value)
    }
}
