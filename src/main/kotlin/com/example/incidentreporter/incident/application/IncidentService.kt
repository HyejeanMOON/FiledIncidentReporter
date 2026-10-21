package com.example.incidentreporter.incident.application

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentRepository
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.IncidentNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IncidentService(private val repository: IncidentRepository) {
    @Transactional
    fun delete(id: String) {
        if (!repository.delete(id)) throw IncidentNotFoundException()
    }

    @Transactional
    fun update(id: String, input: IncidentInput): Incident {
        val valid = input.validated()
        val incident = Incident(id, valid.title, valid.location, valid.description, valid.photoUris)
        if (!repository.update(incident)) throw IncidentNotFoundException()
        return incident
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): IncidentPage {
        if (page < 0 || size !in 1..100) {
            throw InvalidIncidentException("page must be nonnegative and size must be between 1 and 100")
        }
        return repository.findPage(page, size)
    }

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
