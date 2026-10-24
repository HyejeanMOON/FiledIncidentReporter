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
}
