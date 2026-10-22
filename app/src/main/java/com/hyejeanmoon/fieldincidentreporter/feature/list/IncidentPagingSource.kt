package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentSummary
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentsUseCase
import kotlinx.coroutines.CancellationException

internal class IncidentPagingSource(
    private val getIncidents: GetIncidentsUseCase,
) : PagingSource<Int, IncidentSummary>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, IncidentSummary> {
        val page = params.key ?: 0
        return try {
            val result = getIncidents(page, PAGE_SIZE)
            LoadResult.Page(
                data = result.items,
                prevKey = if (result.page == 0) null else result.page - 1,
                nextKey = if (result.hasNext) result.page + 1 else null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    // Refresh from the newest report after a create, update, or delete.
    override fun getRefreshKey(state: PagingState<Int, IncidentSummary>): Int? = null

    companion object {
        const val PAGE_SIZE = 20
    }
}
