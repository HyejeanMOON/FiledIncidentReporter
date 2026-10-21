package com.example.incidentreporter.incident.application

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IncidentService(private val repository: IncidentRepository) {
    @Transactional
    fun create(input: IncidentInput): Incident {
        val valid = input.validated()
        val incident = Incident(
            id = UUID.randomUUID().toString(),
            title = valid.title,
            location = valid.location,
            description = valid.description,
            photoUris = valid.photoUris,
        )
        repository.insert(incident)
        return incident
    }
}
