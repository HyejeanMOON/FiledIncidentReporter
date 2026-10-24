package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveIncidentUseCaseTest {
    private val repository = FakeIncidentRepository(mutableMapOf("incident-1" to testIncident()))
    private val saveIncident = SaveIncidentUseCase(repository)
    private val photo = IncidentPhoto.Local("content://photos/1")
    private val draft = IncidentDraft("  Title ", "\tRoom", "Details\n", listOf(photo))
    private val trimmed = IncidentDraft("Title", "Room", "Details", listOf(photo))

    @Test
    fun `a draft without an id is created with trimmed fields`() = runTest {
        val created = saveIncident(id = null, draft, deletePhotoIds = listOf("ignored"))

        assertEquals("new", created.id)
        assertEquals(trimmed, IncidentDraft(created.title, created.location, created.description, created.photos))
        assertEquals(emptyList<Any>(), repository.updates)
    }

    @Test
    fun `a draft with an id updates that report with trimmed fields and the removed photos`() = runTest {
        saveIncident(id = "incident-1", draft, deletePhotoIds = listOf("photo-A"))

        assertEquals(Triple("incident-1", trimmed, listOf("photo-A")), repository.updates.single())
    }
}
