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

    private val complete = empty.after(
        IncidentFormUiEvent.TitleChanged("Title"),
        IncidentFormUiEvent.LocationChanged("Room"),
        IncidentFormUiEvent.DescriptionChanged("Details"),
        IncidentFormUiEvent.PhotosSelected(listOf("content://photos/1")),
    )

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

    @Test
    fun `edits revalidate the draft`() {
        assertEquals(listOf(RequiredIncidentField.TITLE), complete.after(IncidentFormUiEvent.TitleChanged("  ")).missingFields)
        assertEquals(emptyList<RequiredIncidentField>(), complete.missingFields)
    }

    @Test
    fun `removing the last photo reveals the photos error`() {
        val photo = complete.photos.single()

        assertEquals(listOf(RequiredIncidentField.PHOTOS), complete.after(IncidentFormUiEvent.PhotoRemoved(photo)).visibleMissingFields)
        assertEquals(listOf(RequiredIncidentField.PHOTOS), complete.after(IncidentFormUiEvent.PhotosRemoved).visibleMissingFields)
    }

    @Test
    fun `cancelling the photo picker keeps the photos already chosen`() {
        val state = complete.after(IncidentFormUiEvent.PhotoInputCancelled)

        assertEquals(complete.photos, state.photos)
        assertEquals(emptySet<RequiredIncidentField>(), state.validatedFields)
    }

    @Test
    fun `leaving a field that was never focused reveals nothing`() {
        val state = empty.after(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.TITLE, false))

        assertEquals(emptyList<RequiredIncidentField>(), state.visibleMissingFields)
    }

    @Test
    fun `submitting a complete draft starts saving and clears the last error`() {
        val state = complete.copy(error = IncidentFailure.SERVER_ERROR).after(IncidentFormUiEvent.Submit)

        assertEquals(true, state.isSaving)
        assertEquals(null, state.error)
        assertEquals(false, state.isSubmitEnabled)
        assertEquals(state, state.after(IncidentFormUiEvent.Submit))
    }

    @Test
    fun `a saved form can no longer be edited`() {
        val saved = reducer.reduce(complete.after(IncidentFormUiEvent.Submit), IncidentFormResult.Saved)
            as IncidentFormUiState.Ready

        assertEquals(false, saved.isSaving)
        assertEquals(true, saved.isSaved)
        assertEquals(saved, saved.after(IncidentFormUiEvent.TitleChanged("Other"), IncidentFormUiEvent.PhotosRemoved))
        assertEquals(false, saved.after(IncidentFormUiEvent.Submit).isSaving)
    }

    @Test
    fun `dismissing the error clears it`() {
        assertEquals(null, complete.copy(error = IncidentFailure.SERVER_ERROR).after(IncidentFormUiEvent.ErrorDismissed).error)
    }

    @Test
    fun `events other than retry are ignored until the form is ready`() {
        val failed = IncidentFormUiState.LoadFailed(IncidentFailure.NETWORK_UNAVAILABLE)

        assertEquals(IncidentFormUiState.Loading, reducer.reduce(IncidentFormUiState.Loading, IncidentFormUiEvent.Retry))
        assertEquals(IncidentFormUiState.Loading, reducer.reduce(IncidentFormUiState.Loading, IncidentFormUiEvent.TitleChanged("Title")))
        assertEquals(failed, reducer.reduce(failed, IncidentFormUiEvent.Submit))
    }

    @Test
    fun `save results are ignored when the form is not ready`() {
        assertEquals(IncidentFormUiState.Loading, reducer.reduce(IncidentFormUiState.Loading, IncidentFormResult.Saved))
        assertEquals(
            IncidentFormUiState.Loading,
            reducer.reduce(IncidentFormUiState.Loading, IncidentFormResult.SaveFailed(IncidentFailure.SERVER_ERROR)),
        )
    }

    @Test
    fun `only stored photos missing from the form are deleted`() {
        val first = IncidentPhoto.Stored("photo-A", "http://example.test/A")
        val second = IncidentPhoto.Stored("photo-B", "http://example.test/B")
        val state = reducer.reduce(IncidentFormUiState.Loading, IncidentFormResult.Loaded(testIncident().copy(photos = listOf(first, second))))
            .after(IncidentFormUiEvent.PhotoRemoved(first), IncidentFormUiEvent.PhotosSelected(listOf("content://photos/new")))

        assertEquals(true, state.isEditing)
        assertEquals(listOf("photo-A"), state.deletedPhotoIds)
        assertEquals(listOf("photo-A", "photo-B"), state.after(IncidentFormUiEvent.PhotosRemoved).deletedPhotoIds)
    }
}
