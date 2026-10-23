package com.hyejeanmoon.fieldincidentreporter.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentEntity
import com.hyejeanmoon.fieldincidentreporter.data.local.toEntity
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncident
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.error.toIncidentFailure
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentApi
import com.hyejeanmoon.fieldincidentreporter.testing.FakeIncidentDao
import com.hyejeanmoon.fieldincidentreporter.testing.testTime
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class IncidentRemoteMediatorTest {
    private val baseUrl = "http://example.test/".toHttpUrl()
    private val state = PagingState<Int, IncidentEntity>(emptyList(), null, PagingConfig(INCIDENT_PAGE_SIZE), 0)

    @Test
    fun `refresh writes only changed reports and keeps the pages after the first`() = runTest {
        val server = reports(0 until 45)
        val dao = FakeIncidentDao(server.map { it.cached() })
        val api = FakeIncidentApi(server)
        val mediator = IncidentRemoteMediator(api, dao, baseUrl)

        assertEquals(false, mediator.load(LoadType.REFRESH, state).endOfPagination())
        assertEquals(emptyList<IncidentEntity>(), dao.upserted)
        assertEquals(emptyList<String>(), dao.deleted)

        val edited = report(3, title = "Edited")
        api.incidents = server.map { if (it.id == edited.id) edited else it }
        mediator.load(LoadType.REFRESH, state)

        assertEquals(listOf(edited.cached()), dao.upserted)
        assertEquals(server.map { it.id }, dao.list.map { it.id })
        assertEquals(listOf(0), api.requestedPages.distinct())
    }

    @Test
    fun `refresh adds new reports and removes ones deleted within the first page`() = runTest {
        val cached = reports(0 until 25)
        val dao = FakeIncidentDao(cached.map { it.cached() })
        val server = listOf(report(-1)) + cached.filterNot { it.id == id(5) }
        val mediator = IncidentRemoteMediator(FakeIncidentApi(server), dao, baseUrl)

        mediator.load(LoadType.REFRESH, state)

        assertEquals(listOf(report(-1).cached()), dao.upserted)
        assertEquals(listOf(id(5)), dao.deleted)
        assertEquals(server.map { it.id }, dao.list.map { it.id })
    }

    @Test
    fun `append syncs past cached pages until it reaches reports that are not cached`() = runTest {
        val server = reports(0 until 65)
        val dao = FakeIncidentDao(server.take(45).map { it.cached() })
        val api = FakeIncidentApi(server)
        val mediator = IncidentRemoteMediator(api, dao, baseUrl)
        mediator.load(LoadType.REFRESH, state)

        assertEquals(false, mediator.load(LoadType.APPEND, state).endOfPagination())
        assertEquals(listOf(0, 1, 2), api.requestedPages)
        assertEquals(server.take(60).map { it.id }, dao.list.map { it.id })

        assertEquals(true, mediator.load(LoadType.APPEND, state).endOfPagination())
        assertEquals(listOf(0, 1, 2, 3), api.requestedPages)
        assertEquals(server.map { it.id }, dao.list.map { it.id })
    }

    @Test
    fun `last page removes cached reports after the server's last report`() = runTest {
        val cached = reports(0 until 30)
        val dao = FakeIncidentDao(cached.map { it.cached() })
        val mediator = IncidentRemoteMediator(FakeIncidentApi(cached.take(25)), dao, baseUrl)
        mediator.load(LoadType.REFRESH, state)

        assertEquals(true, mediator.load(LoadType.APPEND, state).endOfPagination())
        assertEquals(cached.take(25).map { it.id }, dao.list.map { it.id })
    }

    @Test
    fun `failed request keeps the cache and reports the failure`() = runTest {
        val cached = reports(0 until 5).map { it.cached() }
        val dao = FakeIncidentDao(cached)
        val api = FakeIncidentApi(emptyList()).apply { failure = IOException("offline") }

        val result = IncidentRemoteMediator(api, dao, baseUrl).load(LoadType.REFRESH, state)

        assertEquals(IncidentFailure.NETWORK, (result as MediatorResult.Error).throwable.toIncidentFailure())
        assertEquals(cached, dao.list)
    }

    private fun MediatorResult.endOfPagination() = (this as MediatorResult.Success).endOfPaginationReached

    private fun IncidentDto.cached() = toIncident(baseUrl).toEntity()

    private fun reports(indices: IntRange) = indices.map { report(it) }

    /** A higher [index] is an older report, so it comes later in the list. */
    private fun report(index: Int, title: String = "Report $index"): IncidentDto {
        val time = testTime.minusSeconds(index * 60L).toString()
        return IncidentDto(id(index), title, "Room", "Details", createdAt = time, updatedAt = time)
    }

    private fun id(index: Int) = "incident-%03d".format(index)
}
