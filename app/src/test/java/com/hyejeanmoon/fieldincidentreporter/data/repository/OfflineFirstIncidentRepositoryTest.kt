package com.hyejeanmoon.fieldincidentreporter.data.repository

import android.content.ContextWrapper
import com.hyejeanmoon.fieldincidentreporter.data.local.toEntity
import com.hyejeanmoon.fieldincidentreporter.data.local.toIncident
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPhotoDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncident
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import com.hyejeanmoon.fieldincidentreporter.domain.error.toIncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentApi
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentDao
import com.hyejeanmoon.fieldincidentreporter.testing.testTime
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException
import java.time.Instant

class OfflineFirstIncidentRepositoryTest {
    private val baseUrl = "http://example.test/"
    private val cached = report(title = "Before")

    @Test
    fun `lookup refreshes the cached report`() = runTest {
        val dao = FakeIncidentDao(listOf(cached.cached()))
        val edited = report(title = "Edited on the web", updatedAt = testTime.plusSeconds(60))

        assertEquals("Edited on the web", repository(FakeIncidentApi(listOf(edited)), dao).getIncident(cached.id).title)
        assertEquals(listOf(edited.cached()), dao.list)
    }

    @Test
    fun `lookup does not add a report that is not cached`() = runTest {
        val dao = FakeIncidentDao()

        repository(FakeIncidentApi(listOf(cached)), dao).getIncident(cached.id)

        assertEquals(emptyList<String>(), dao.list.map { it.id })
    }

    @Test
    fun `lookup of a report deleted on the server removes it from the cache`() = runTest {
        val dao = FakeIncidentDao(listOf(cached.cached()))

        val result = runSuspendCatching { repository(FakeIncidentApi(emptyList()), dao).getIncident(cached.id) }

        assertEquals(IncidentFailure.INCIDENT_NOT_FOUND, result.exceptionOrNull()?.toIncidentFailure())
        assertEquals(listOf(cached.id), dao.deleted)
        assertEquals(emptyList<String>(), dao.list.map { it.id })
    }

    @Test
    fun `failed lookup keeps the cached report`() = runTest {
        val dao = FakeIncidentDao(listOf(cached.cached()))
        val api = FakeIncidentApi(listOf(cached)).apply { failure = IOException("offline") }

        val result = runSuspendCatching { repository(api, dao).getIncident(cached.id) }

        assertEquals(IncidentFailure.NETWORK_UNAVAILABLE, result.exceptionOrNull()?.toIncidentFailure())
        assertEquals(listOf(cached.cached()), dao.list)
    }

    @Test
    fun `create caches the new report`() = runTest {
        val dao = FakeIncidentDao(listOf(cached.cached()))
        val api = FakeIncidentApi(listOf(cached))

        val created = repository(api, dao).create(IncidentDraft("Leak", "Bay", "Pipe", emptyList()))

        assertEquals(Triple(null, IncidentBody("Leak", "Bay", "Pipe"), 0), api.writes.single())
        assertEquals(listOf(created.id, cached.id), dao.list.map { it.id })
        assertEquals(created, dao.list.first().toIncident())
    }

    @Test
    fun `update sends the removed photos, uploads none of the stored ones, and caches the result`() = runTest {
        val photo = IncidentPhotoDto("photo-A", "/incidents/incident-1/photos/photo-A", "/thumbnail")
        val removed = photo.copy(id = "photo-B")
        val report = cached.copy(photos = listOf(photo, removed))
        val dao = FakeIncidentDao(listOf(report.cached()))
        val api = FakeIncidentApi(listOf(report))
        val kept = report.toIncident(baseUrl.toHttpUrl()).photos.first()
        val draft = IncidentDraft("After", "Room", "Details", listOf(kept))

        val updated = repository(api, dao).update(cached.id, draft, deletePhotoIds = listOf("photo-B"))

        assertEquals(Triple(cached.id, IncidentBody("After", "Room", "Details", listOf("photo-B")), 0), api.writes.single())
        assertEquals(listOf(kept), updated.photos)
        assertEquals(listOf(updated), dao.list.map { it.toIncident() })
    }

    @Test
    fun `failed save leaves the cache alone`() = runTest {
        val dao = FakeIncidentDao(listOf(cached.cached()))
        val api = FakeIncidentApi(listOf(cached)).apply { failure = IOException("offline") }

        val draft = IncidentDraft("After", "Room", "Details", emptyList())

        val result = runSuspendCatching { repository(api, dao).update(cached.id, draft, emptyList()) }

        assertEquals(IncidentFailure.NETWORK_UNAVAILABLE, result.exceptionOrNull()?.toIncidentFailure())
        assertEquals(listOf(cached.cached()), dao.list)
    }

    @Test
    fun `delete removes the report from the cache`() = runTest {
        val dao = FakeIncidentDao(listOf(cached.cached()))

        repository(FakeIncidentApi(listOf(cached)), dao).delete(cached.id)

        assertEquals(emptyList<String>(), dao.list.map { it.id })
    }

    @Test
    fun `failed delete keeps the cached report and reports the HTTP failure`() = runTest {
        val dao = FakeIncidentDao(listOf(cached.cached()))

        val result = runSuspendCatching { repository(FakeIncidentApi(emptyList()), dao).delete(cached.id) }

        assertEquals(IncidentFailure.INCIDENT_NOT_FOUND, result.exceptionOrNull()?.toIncidentFailure())
        assertEquals(listOf(cached.cached()), dao.list)
    }

    // These tests never read photos from the device, so the context is never used.
    private fun repository(api: FakeIncidentApi, dao: FakeIncidentDao) =
        OfflineFirstIncidentRepository(api, Retrofit.Builder().baseUrl(baseUrl).build(), ContextWrapper(null), dao)

    private fun IncidentDto.cached() = toIncident(baseUrl.toHttpUrl()).toEntity()

    private fun report(title: String, updatedAt: Instant = testTime) =
        IncidentDto("incident-1", title, "Room", "Details", createdAt = testTime.toString(), updatedAt = updatedAt.toString())
}
