package com.example.incidentreporter.incident.application

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentRepository
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.IncidentNotFoundException
import com.example.incidentreporter.incident.domain.IncomingPhoto
import com.example.incidentreporter.incident.domain.PhotoStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
class IncidentService(
    private val repository: IncidentRepository,
    private val photoStore: PhotoStore,
) {
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
    fun create(input: IncidentInput, photos: List<IncomingPhoto>): Incident {
        val valid = input.validated(requirePhotos = false)
        if (photos.size !in 1..20) throw InvalidIncidentException("An incident needs 1 to 20 photos")
        val stored = mutableListOf<String>()
        try {
            photos.forEach { stored += photoStore.save(it) }
            val incident = Incident(
                id = UUID.randomUUID().toString(),
                title = valid.title,
                location = valid.location,
                description = valid.description,
                photoUris = stored.toList(),
            )
            repository.insert(incident)
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) stored.forEach(photoStore::delete)
                }
            })
            return incident
        } catch (error: Exception) {
            stored.forEach(photoStore::delete)
            throw error
        }
    }
}
