package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hyejeanmoon.fieldincidentreporter.domain.usecase.GetIncidentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    getIncidents: GetIncidentsUseCase,
) : ViewModel() {
    // The list reads Room, so writes elsewhere in the app show up without reloading it here.
    val incidents = getIncidents().cachedIn(viewModelScope)
}
