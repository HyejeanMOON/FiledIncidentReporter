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
    private var nextCursor: String? = null
    private var lastSynced: IncidentEntity? = null

    override suspend fun load(loadType: LoadType, state: PagingState<Int, IncidentEntity>): MediatorResult {
        var cursor = when (loadType) {
            LoadType.REFRESH -> null
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> nextCursor ?: return MediatorResult.Success(endOfPaginationReached = true)
        }
        return runSuspendCatching<MediatorResult> {
            do {
                val response = incidentRequest { api.getIncidents(cursor, INCIDENT_PAGE_SIZE) }
                val rows = response.items.map { it.toIncident(baseUrl).toEntity() }
                // A cursor continues right after the last report synced, so no cached row falls between pages.
                val previous = if (cursor == null) null else lastSynced
                val cachedFromLast = dao.syncPage(rows, previous, isLastPage = response.nextCursor == null)
                lastSynced = rows.lastOrNull() ?: previous
                nextCursor = response.nextCursor
                cursor = response.nextCursor
                // Until a page reaches past the cached rows, the end of the list has not moved, so keep syncing.
            } while (loadType == LoadType.APPEND && cursor != null && cachedFromLast)
            MediatorResult.Success(endOfPaginationReached = nextCursor == null)
        }.getOrElse(MediatorResult::Error)
    }
}
