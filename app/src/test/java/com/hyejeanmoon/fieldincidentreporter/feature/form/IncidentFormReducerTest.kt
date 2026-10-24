package com.hyejeanmoon.fieldincidentreporter.feature.form

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ValidateIncidentDraftUseCase
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentFormReducerTest {
    private val reducer = IncidentFormReducer(ValidateIncidentDraftUseCase())
    private val empty = reducer.validated(IncidentFormUiState())

    private fun IncidentFormUiState.after(vararg events: IncidentFormUiEvent) =
        events.fold(this, reducer::reduce)

    @Test
    fun `leaving a field reveals its error but focusing it does not`() {
        val focused = empty.after(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.TITLE, true))
        assertEquals(emptyList<RequiredIncidentField>(), focused.visibleMissingFields)

        val left = focused.after(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.TITLE, false))
        assertEquals(listOf(RequiredIncidentField.TITLE), left.visibleMissingFields)
    }

    @Test
    fun `cancelling the photo picker with no photos reveals the photos error`() {
        val state = empty.after(IncidentFormUiEvent.PhotoInputCancelled)
        assertEquals(listOf(RequiredIncidentField.PHOTOS), state.visibleMissingFields)
    }

    @Test
    fun `selected photos drop blanks and duplicates`() {
        val state = empty.after(
            IncidentFormUiEvent.PhotosSelected(listOf("content://photos/1", " ")),
            IncidentFormUiEvent.PhotosSelected(listOf("content://photos/1")),
        )
        assertEquals(listOf(IncidentPhoto.Local("content://photos/1")), state.photos)
    }

    @Test
    fun `an incomplete draft is not submitted`() {
        assertEquals(false, empty.after(IncidentFormUiEvent.TitleChanged("Title"), IncidentFormUiEvent.Submit).isSaving)
    }

    @Test
    fun `edits are ignored while saving`() {
        val saving = empty.copy(isSaving = true)
        assertEquals("", reducer.reduce(saving, IncidentFormUiEvent.TitleChanged("Title")).title)
    }

    @Test
    fun `a loaded incident fills a valid form`() {
        val photo = IncidentPhoto.Stored("photo-A", "http://example.test/A")
        val loading = reducer.validated(IncidentFormUiState(incidentId = "incident-1", isLoading = true))

        val state = reducer.reduce(loading, IncidentFormResult.Loaded(testIncident().copy(photos = listOf(photo))))
        assertEquals(false, state.isLoading)
        assertEquals(listOf("photo-A"), state.originalPhotoIds)
        assertEquals(true, state.isSubmitEnabled)
    }

    @Test
    fun `retry restarts loading only after a load failure`() {
        val loading = IncidentFormUiState(incidentId = "incident-1", isLoading = true)
        val failed = reducer.reduce(loading, IncidentFormResult.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE))

        val retried = failed.after(IncidentFormUiEvent.Retry)
        assertEquals(true, retried.isLoading)
        assertEquals(null, retried.loadFailure)
        assertEquals(false, empty.after(IncidentFormUiEvent.Retry).isLoading)
    }

    @Test
    fun `a failed save keeps the form editable`() {
        val saving = empty.copy(isSaving = true)
        val state = reducer.reduce(saving, IncidentFormResult.SaveFailed(IncidentFailure.SERVER_ERROR))
        assertEquals(IncidentFailure.SERVER_ERROR, state.error)
        assertEquals(true, state.canEdit)
    }
}
