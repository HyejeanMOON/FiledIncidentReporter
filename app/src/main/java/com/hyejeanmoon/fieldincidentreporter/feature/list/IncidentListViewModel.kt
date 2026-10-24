package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    repository: IncidentRepository,
) : ViewModel() {
    // The list reads Room, so writes elsewhere in the app show up without reloading it here.
    val incidents = repository.incidents().cachedIn(viewModelScope)
}
