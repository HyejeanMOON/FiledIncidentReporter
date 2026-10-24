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
    private val empty = reducer.validated(IncidentFormUiState.Ready())

    private fun IncidentFormUiState.after(vararg events: IncidentFormUiEvent) =
        events.fold(this, reducer::reduce) as IncidentFormUiState.Ready

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
        assertEquals("", empty.copy(isSaving = true).after(IncidentFormUiEvent.TitleChanged("Title")).title)
    }

    @Test
    fun `a loaded incident fills a valid form`() {
        val photo = IncidentPhoto.Stored("photo-A", "http://example.test/A")
        val incident = testIncident().copy(photos = listOf(photo))

        val state = reducer.reduce(IncidentFormUiState.Loading, IncidentFormResult.Loaded(incident))
            as IncidentFormUiState.Ready
        assertEquals("incident-1", state.incidentId)
        assertEquals(listOf("photo-A"), state.originalPhotoIds)
        assertEquals(true, state.isSubmitEnabled)
    }

    @Test
    fun `retry restarts loading only after a load failure`() {
        val failed = reducer.reduce(
            IncidentFormUiState.Loading,
            IncidentFormResult.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE),
        )

        assertEquals(IncidentFormUiState.Loading, reducer.reduce(failed, IncidentFormUiEvent.Retry))
        assertEquals(empty, empty.after(IncidentFormUiEvent.Retry))
    }

    @Test
    fun `a failed save keeps the form editable`() {
        val state = reducer.reduce(empty.copy(isSaving = true), IncidentFormResult.SaveFailed(IncidentFailure.SERVER_ERROR))
            as IncidentFormUiState.Ready
        assertEquals(IncidentFailure.SERVER_ERROR, state.error)
        assertEquals(true, state.canEdit)
    }
}
