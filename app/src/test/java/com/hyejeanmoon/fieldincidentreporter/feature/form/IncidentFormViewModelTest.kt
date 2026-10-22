package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPage
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.CreateIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.UpdateIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ValidateIncidentDraftUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentFormViewModelTest {
    @Test
    fun `mixed photo edit sends only removed stored ids and keeps new local photo`() {
        val first = IncidentPhoto.Stored("photo-A", "http://example.test/A")
        val second = IncidentPhoto.Stored("photo-B", "http://example.test/B")
        val loaded = IncidentFormUiStateReducer.reduce(
            IncidentFormUiState(),
            IncidentFormStateChange.Initialized(Incident("incident-1", "Title", "Room", "Details", listOf(first, second))),
        )
        val removed = IncidentFormUiStateReducer.reduce(loaded, IncidentFormStateChange.PhotoRemoved(second))
        val updated = IncidentFormUiStateReducer.reduce(
            removed,
            IncidentFormStateChange.PhotosSelected(listOf("content://photos/new")),
        )

        assertEquals(listOf("photo-B"), updated.deletedPhotoIds)
        assertEquals(listOf(first, IncidentPhoto.Local("content://photos/new")), updated.photos)
    }

    @Test
    fun `editing validates the latest form state before exposing it`() {
        val repository = UnusedIncidentRepository()
        val viewModel = IncidentFormViewModel(
            CreateIncidentUseCase(repository),
            GetIncidentUseCase(repository),
            UpdateIncidentUseCase(repository),
            ValidateIncidentDraftUseCase(),
            SavedStateHandle(mapOf("form_initialized" to true)),
        )

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
    fun `restored edit keeps stored references distinct from local photos`() {
        val handle = SavedStateHandle(
            mapOf(
                "form_initialized" to true,
                "form_incident_id" to "incident-1",
                "form_photo_types" to arrayListOf("stored", "local"),
                "form_photo_references" to arrayListOf("photo-1", "content://photos/2"),
                "form_photo_display_uris" to arrayListOf("http://example.test/photos/1.jpg", "content://photos/2"),
                "form_original_photo_ids" to arrayListOf("photo-1"),
            ),
        )
        val repository = UnusedIncidentRepository()
        val viewModel = IncidentFormViewModel(
            CreateIncidentUseCase(repository),
            GetIncidentUseCase(repository),
            UpdateIncidentUseCase(repository),
            ValidateIncidentDraftUseCase(),
            handle,
        )

        assertEquals(
            listOf(
                IncidentPhoto.Stored("photo-1", "http://example.test/photos/1.jpg"),
                IncidentPhoto.Local("content://photos/2"),
            ),
            viewModel.uiState.value.photos,
        )
        viewModel.onEvent(IncidentFormUiEvent.PhotoRemoved(viewModel.uiState.value.photos.first()))
        assertEquals(listOf("photo-1"), viewModel.uiState.value.deletedPhotoIds)
        assertEquals(listOf(IncidentPhoto.Local("content://photos/2")), viewModel.uiState.value.photos)
    }
}

private class UnusedIncidentRepository : IncidentRepository {
    override suspend fun getIncidentsPage(page: Int, size: Int): IncidentPage = error("Not used")
    override suspend fun getIncident(id: String): Incident = error("Not used")
    override suspend fun create(title: String, location: String, description: String, photos: List<IncidentPhoto>): Incident = error("Not used")
    override suspend fun update(incident: Incident, deletePhotoIds: List<String>): Incident = error("Not used")
    override suspend fun delete(id: String): Unit = error("Not used")
}
