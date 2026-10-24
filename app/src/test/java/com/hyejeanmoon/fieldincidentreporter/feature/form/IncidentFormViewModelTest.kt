package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentException
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.SaveIncidentUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ValidateIncidentDraftUseCase
import com.hyejeanmoon.fieldincidentreporter.testing.FakeCapturedPhotoStore
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.testing.MainDispatcherRule
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class IncidentFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val first = IncidentPhoto.Stored("photo-A", "http://example.test/A")
    private val second = IncidentPhoto.Stored("photo-B", "http://example.test/B")
    private val repository = FakeIncidentRepository(
        mutableMapOf("incident-1" to testIncident().copy(photos = listOf(first, second))),
    )

    private val reducer = IncidentFormReducer(ValidateIncidentDraftUseCase())
    private val capturedPhotos = FakeCapturedPhotoStore()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = IncidentFormViewModel(
        handle,
        repository,
        SaveIncidentUseCase(repository),
        reducer,
        capturedPhotos,
    )

    @Test
    fun `closing the form deletes the photos the camera captured for it`() {
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(store, viewModelFactory { initializer { viewModel() } })
            .get(IncidentFormViewModel::class)
        val captured = viewModel.newCapturedPhotoUri()
        viewModel.onEvent(IncidentFormUiEvent.PhotosSelected(listOf(captured)))
        assertEquals(listOf(captured), capturedPhotos.photos)

        store.clear()

        assertEquals(emptyList<String>(), capturedPhotos.photos)
    }

    @Test
    fun `restored edit keeps the thumbnails of stored photos`() {
        val stored = IncidentPhoto.Stored("photo-A", "http://example.test/A", "http://example.test/A/thumbnail")
        val form = SavedForm.from(IncidentFormUiState.Ready(incidentId = "incident-1", photos = listOf(stored)))
        val handle = SavedStateHandle(
            mapOf("incidentId" to "incident-1", IncidentFormViewModel.SAVED_FORM_KEY to Json.encodeToString(form)),
        )

        assertEquals(listOf(stored), viewModel(handle).form.photos)
    }

    private val IncidentFormViewModel.form get() = uiState.value as IncidentFormUiState.Ready

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
        assertEquals(true, viewModel.form.isSaved)
    }

    @Test
    fun `submit is enabled only for a complete draft`() {
        val viewModel = viewModel()
        viewModel.onEvent(IncidentFormUiEvent.TitleChanged("Title"))
        viewModel.onEvent(IncidentFormUiEvent.LocationChanged("Room"))
        viewModel.onEvent(IncidentFormUiEvent.DescriptionChanged("Description"))
        assertEquals(false, viewModel.form.isSubmitEnabled)

        viewModel.onEvent(IncidentFormUiEvent.PhotosSelected(listOf("content://photos/1")))
        assertEquals(true, viewModel.form.isSubmitEnabled)

        viewModel.onEvent(IncidentFormUiEvent.PhotosRemoved)
        assertEquals(false, viewModel.form.isSubmitEnabled)
    }

    @Test
    fun `restored edit keeps stored photos distinct from local photos`() {
        val handle = SavedStateHandle(mapOf("incidentId" to "incident-1"))
        viewModel(handle).onEvent(IncidentFormUiEvent.PhotosSelected(listOf("content://photos/2")))

        // A new ViewModel on the same handle simulates process death.
        val restored = viewModel(handle)
        assertEquals(listOf(first, second, IncidentPhoto.Local("content://photos/2")), restored.form.photos)

        restored.onEvent(IncidentFormUiEvent.PhotoRemoved(first))
        assertEquals(listOf("photo-A"), restored.form.deletedPhotoIds)
    }

    @Test
    fun `a new report starts empty and is created on submit`() = runTest {
        val viewModel = viewModel()
        assertEquals(false, viewModel.form.isEditing)
        assertEquals(RequiredIncidentField.entries, viewModel.form.missingFields)

        viewModel.fill()
        viewModel.onEvent(IncidentFormUiEvent.Submit)

        assertEquals(true, viewModel.form.isSaved)
        assertEquals(emptyList<Any>(), repository.updates)
        assertEquals("Leak", repository.getIncident("new").title)
    }

    @Test
    fun `editing loads the report into the form`() {
        val form = viewModel(editHandle()).form

        assertEquals(true, form.isEditing)
        assertEquals("Title", form.title)
        assertEquals(listOf(first, second), form.photos)
        assertEquals(true, form.isSubmitEnabled)
    }

    @Test
    fun `failed load of the report to edit can be retried`() {
        repository.failure = IncidentException(IncidentFailure.NETWORK_UNAVAILABLE, IOException())
        val viewModel = viewModel(editHandle())
        assertEquals(IncidentFormUiState.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE), viewModel.uiState.value)

        repository.failure = null
        viewModel.onEvent(IncidentFormUiEvent.Retry)

        assertEquals("Title", viewModel.form.title)
    }

    @Test
    fun `failed save shows the error and keeps the edits`() {
        val viewModel = viewModel()
        viewModel.fill()
        repository.failure = IncidentException(IncidentFailure.UPLOAD_TOO_LARGE, IOException())
        viewModel.onEvent(IncidentFormUiEvent.Submit)

        assertEquals(IncidentFailure.UPLOAD_TOO_LARGE, viewModel.form.error)
        assertEquals("Leak", viewModel.form.title)
        assertEquals(true, viewModel.form.isSubmitEnabled)

        repository.failure = null
        viewModel.onEvent(IncidentFormUiEvent.ErrorDismissed)
        viewModel.onEvent(IncidentFormUiEvent.Submit)
        assertEquals(true, viewModel.form.isSaved)
    }

    @Test
    fun `restored form keeps the errors already revealed and skips loading`() {
        val handle = editHandle()
        viewModel(handle).run {
            onEvent(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.TITLE, true))
            onEvent(IncidentFormUiEvent.TitleChanged(""))
            onEvent(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.TITLE, false))
        }

        // Loading would replace the restored edits, so a failing repository shows it is not called.
        repository.failure = IncidentException(IncidentFailure.NETWORK_UNAVAILABLE, IOException())
        val restored = viewModel(handle)
        assertEquals("", restored.form.title)
        assertEquals(listOf(RequiredIncidentField.TITLE), restored.form.visibleMissingFields)
    }

    @Test
    fun `an unreadable saved form is ignored`() {
        val viewModel = viewModel(SavedStateHandle(mapOf(IncidentFormViewModel.SAVED_FORM_KEY to "not json")))

        assertEquals(reducer.validated(IncidentFormUiState.Ready()), viewModel.uiState.value)
    }

    private fun editHandle() = SavedStateHandle(mapOf("incidentId" to "incident-1"))

    private fun IncidentFormViewModel.fill() {
        onEvent(IncidentFormUiEvent.TitleChanged("Leak"))
        onEvent(IncidentFormUiEvent.LocationChanged("Bay"))
        onEvent(IncidentFormUiEvent.DescriptionChanged("Pipe"))
        onEvent(IncidentFormUiEvent.PhotosSelected(listOf("content://photos/1")))
    }
}
