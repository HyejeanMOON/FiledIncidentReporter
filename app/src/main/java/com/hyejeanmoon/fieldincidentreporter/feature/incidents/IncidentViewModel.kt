package com.hyejeanmoon.fieldincidentreporter.feature.incidents

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class IncidentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(IncidentUiState())
    val uiState = _uiState.asStateFlow()

    fun saveIncident(
        id: String?,
        title: String,
        location: String,
        description: String,
        photoUris: List<String>,
    ) {
        val current = _uiState.value
        val existing = id?.let { candidate -> current.incidents.firstOrNull { it.id == candidate } }
        val incident = Incident(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = title,
            location = location,
            description = description,
            photoUris = photoUris,
        )
        val updatedIncidents = if (existing == null) {
            listOf(incident) + current.incidents
        } else {
            current.incidents.map { currentIncident ->
                if (currentIncident.id == existing.id) incident else currentIncident
            }
        }
        _uiState.value = current.copy(incidents = updatedIncidents)
    }

    fun removeIncident(id: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            incidents = current.incidents.filterNot { it.id == id },
        )
    }
}
