package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.lifecycle.SavedStateHandle
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentException
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentChange
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.DeleteIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ObserveIncidentChangesUseCase
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.testing.MainDispatcherRule
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import kotlinx.coroutines.test.runTest
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
        GetIncidentUseCase(repository),
        DeleteIncidentUseCase(repository),
        ObserveIncidentChangesUseCase(repository),
    )

    @Test
    fun `saved edit of the same report replaces the shown report`() = runTest {
        val viewModel = viewModel()
        assertEquals("Title", viewModel.uiState.value.incident?.title)

        repository.changes.emit(IncidentChange.Saved(testIncident(id = "other", title = "Other")))
        repository.changes.emit(IncidentChange.Saved(testIncident(title = "Edited")))

        assertEquals("Edited", viewModel.uiState.value.incident?.title)
    }

    @Test
    fun `failed load can be retried`() {
        repository.failure = IncidentException(IncidentFailure.NETWORK, IOException())
        val viewModel = viewModel()
        assertEquals(IncidentFailure.NETWORK, viewModel.uiState.value.loadFailure)

        repository.failure = null
        viewModel.onAction(IncidentDetailAction.Retry)

        assertEquals(null, viewModel.uiState.value.loadFailure)
        assertEquals("incident-1", viewModel.uiState.value.incident?.id)
    }

    @Test
    fun `delete keeps controls disabled and reports completion`() {
        val viewModel = viewModel()
        viewModel.onAction(IncidentDetailAction.Delete)

        assertEquals(true, viewModel.uiState.value.isDeleted)
        assertEquals(true, viewModel.uiState.value.isDeleting)
    }
}
