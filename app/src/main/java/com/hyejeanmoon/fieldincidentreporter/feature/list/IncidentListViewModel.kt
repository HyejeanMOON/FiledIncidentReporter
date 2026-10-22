package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    private val getIncidents: GetIncidentsUseCase,
) : ViewModel() {
    val incidents = Pager(
        config = PagingConfig(
            pageSize = IncidentPagingSource.PAGE_SIZE,
            initialLoadSize = IncidentPagingSource.PAGE_SIZE,
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { IncidentPagingSource(getIncidents) },
    ).flow.cachedIn(viewModelScope)
}
