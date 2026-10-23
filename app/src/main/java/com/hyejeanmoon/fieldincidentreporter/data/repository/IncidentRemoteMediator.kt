package com.hyejeanmoon.fieldincidentreporter.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentDao
import com.hyejeanmoon.fieldincidentreporter.data.local.IncidentEntity
import com.hyejeanmoon.fieldincidentreporter.data.local.toEntity
import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.incidentRequest
import com.hyejeanmoon.fieldincidentreporter.data.remote.toIncident
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import okhttp3.HttpUrl

internal const val INCIDENT_PAGE_SIZE = 20

/**
 * Syncs server pages into Room, which the list reads. A refresh syncs the first page; an append
 * syncs the pages after it, skipping past pages that are already cached, until the cache grows.
 */
@OptIn(ExperimentalPagingApi::class)
internal class IncidentRemoteMediator(
    private val api: IncidentApi,
    private val dao: IncidentDao,
    private val baseUrl: HttpUrl,
) : RemoteMediator<Int, IncidentEntity>() {
    // Every Pager starts with a refresh, so the position only needs to last as long as this mediator.
    private var nextPage: Int? = null
    private var lastSynced: IncidentEntity? = null

    override suspend fun load(loadType: LoadType, state: PagingState<Int, IncidentEntity>): MediatorResult {
        val startPage = when (loadType) {
            LoadType.REFRESH -> 0
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> nextPage ?: return MediatorResult.Success(endOfPaginationReached = true)
        }
        return runSuspendCatching<MediatorResult> {
            var page = startPage
            do {
                val response = incidentRequest { api.getIncidents(page, INCIDENT_PAGE_SIZE) }
                val rows = response.items.map { it.toIncident(baseUrl).toEntity() }
                val previous = if (page == 0) null else lastSynced
                val cachedFromLast = dao.syncPage(rows, previous, isLastPage = !response.hasNext)
                lastSynced = rows.lastOrNull() ?: previous
                nextPage = (page + 1).takeIf { response.hasNext }
                page++
                // Until a page reaches past the cached rows, the end of the list has not moved, so keep syncing.
            } while (loadType == LoadType.APPEND && nextPage != null && cachedFromLast)
            MediatorResult.Success(endOfPaginationReached = nextPage == null)
        }.getOrElse(MediatorResult::Error)
    }
}
