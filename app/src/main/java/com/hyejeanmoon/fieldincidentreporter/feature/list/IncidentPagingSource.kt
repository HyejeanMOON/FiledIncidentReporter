package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hyejeanmoon.fieldincidentreporter.domain.error.runSuspendCatching
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentsUseCase

internal class IncidentPagingSource(
    private val getIncidents: GetIncidentsUseCase,
) : PagingSource<Int, Incident>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Incident> {
        val page = params.key ?: 0
        return runSuspendCatching { getIncidents(page, PAGE_SIZE) }.fold(
            onSuccess = { result ->
                LoadResult.Page(
                    data = result.items,
                    prevKey = if (result.page == 0) null else result.page - 1,
                    nextKey = if (result.hasNext) result.page + 1 else null,
                )
            },
            onFailure = { error -> LoadResult.Error(error) },
        )
    }

    // Refresh from the newest report after a create, update, or delete.
    override fun getRefreshKey(state: PagingState<Int, Incident>): Int? = null

    companion object {
        const val PAGE_SIZE = 20
    }
}
