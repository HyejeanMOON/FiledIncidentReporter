package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.SaveIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ValidateIncidentDraftUseCase
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.testing.MainDispatcherRule
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IncidentFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val first = IncidentPhoto.Stored("photo-A", "http://example.test/A")
    private val second = IncidentPhoto.Stored("photo-B", "http://example.test/B")
    private val repository = FakeIncidentRepository(
        mutableMapOf("incident-1" to testIncident().copy(photos = listOf(first, second))),
    )

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = IncidentFormViewModel(
        handle,
        repository,
        SaveIncidentUseCase(repository),
        IncidentFormReducer(ValidateIncidentDraftUseCase()),
    )

    @Test
    fun `mixed photo edit sends only removed stored ids and new local photos`() {
        val viewModel = viewModel(SavedStateHandle(mapOf("incidentId" to "incident-1")))
        viewModel.onEvent(IncidentFormUiEvent.PhotoRemoved(second))
        viewModel.onEvent(IncidentFormUiEvent.PhotosSelected(listOf("content://photos/new")))
        viewModel.onEvent(IncidentFormUiEvent.Submit)

        val (id, draft, deletedIds) = repository.updates.single()
        assertEquals("incident-1", id)
        assertEquals(listOf("photo-B"), deletedIds)
        assertEquals(listOf(first, IncidentPhoto.Local("content://photos/new")), draft.photos)
        assertEquals(true, viewModel.uiState.value.isSaved)
    }

    @Test
    fun `submit is enabled only for a complete draft`() {
        val viewModel = viewModel()
        viewModel.onEvent(IncidentFormUiEvent.TitleChanged("Title"))
        viewModel.onEvent(IncidentFormUiEvent.LocationChanged("Room"))
        viewModel.onEvent(IncidentFormUiEvent.DescriptionChanged("Description"))
        assertEquals(false, viewModel.uiState.value.isSubmitEnabled)

        viewModel.onEvent(IncidentFormUiEvent.PhotosSelected(listOf("content://photos/1")))
        assertEquals(true, viewModel.uiState.value.isSubmitEnabled)

        viewModel.onEvent(IncidentFormUiEvent.PhotosRemoved)
        assertEquals(false, viewModel.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `restored edit keeps stored photos distinct from local photos`() {
        val handle = SavedStateHandle(mapOf("incidentId" to "incident-1"))
        viewModel(handle).onEvent(IncidentFormUiEvent.PhotosSelected(listOf("content://photos/2")))

        // A new ViewModel on the same handle simulates process death.
        val restored = viewModel(handle)
        assertEquals(listOf(first, second, IncidentPhoto.Local("content://photos/2")), restored.uiState.value.photos)

        restored.onEvent(IncidentFormUiEvent.PhotoRemoved(first))
        assertEquals(listOf("photo-A"), restored.uiState.value.deletedPhotoIds)
    }
}
