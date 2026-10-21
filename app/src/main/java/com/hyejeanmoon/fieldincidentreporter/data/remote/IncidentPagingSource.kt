package com.hyejeanmoon.fieldincidentreporter.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import kotlinx.coroutines.CancellationException

internal class IncidentPagingSource(private val api: IncidentApi) : PagingSource<Int, Incident>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Incident> {
        val page = params.key ?: 0
        return try {
            val result = incidentRequest { api.getIncidents(page, PAGE_SIZE) }
            LoadResult.Page(
                data = result.items.map { it.toIncident() },
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (result.hasNext) page + 1 else null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    // Refresh from the newest report after a create, update, or delete.
    override fun getRefreshKey(state: PagingState<Int, Incident>): Int? = null

    companion object {
        const val PAGE_SIZE = 20
    }
}
