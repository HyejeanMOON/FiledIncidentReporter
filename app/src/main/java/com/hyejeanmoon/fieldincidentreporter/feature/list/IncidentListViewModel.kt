package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentsUseCase
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.ObserveIncidentChangesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    getIncidents: GetIncidentsUseCase,
    observeIncidentChanges: ObserveIncidentChangesUseCase,
) : ViewModel() {
    private val pagingSourceFactory = InvalidatingPagingSourceFactory { IncidentPagingSource(getIncidents) }

    val incidents = Pager(
        config = PagingConfig(
            pageSize = IncidentPagingSource.PAGE_SIZE,
            initialLoadSize = IncidentPagingSource.PAGE_SIZE,
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = pagingSourceFactory,
    ).flow.cachedIn(viewModelScope)

    init {
        // Any successful write elsewhere in the app reloads the list.
        observeIncidentChanges()
            .onEach { pagingSourceFactory.invalidate() }
            .launchIn(viewModelScope)
    }
}
