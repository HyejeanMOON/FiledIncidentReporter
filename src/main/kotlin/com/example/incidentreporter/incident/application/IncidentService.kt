package com.example.incidentreporter.incident.application

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentPhoto
import com.example.incidentreporter.incident.domain.IncidentRepository
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.IncidentNotFoundException
import com.example.incidentreporter.incident.domain.PhotoNotFoundException
import com.example.incidentreporter.incident.domain.IncomingPhoto
import com.example.incidentreporter.incident.domain.PhotoStore
import com.example.incidentreporter.incident.domain.StoredPhoto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Service
class IncidentService(
    private val repository: IncidentRepository,
    private val photoStore: PhotoStore,
) {
    @Transactional(readOnly = true)
    fun get(id: String): Incident = repository.findById(id) ?: throw IncidentNotFoundException()

    @Transactional(readOnly = true)
    fun getPhoto(incidentId: String, photoId: String): StoredPhoto {
        val photo = get(incidentId).photos.firstOrNull { it.id == photoId } ?: throw PhotoNotFoundException()
        return photoStore.load(photo.uri) ?: throw PhotoNotFoundException()
    }

    @Transactional
    fun delete(id: String) {
        val incident = repository.findById(id) ?: throw IncidentNotFoundException()
        if (!repository.delete(id)) throw IncidentNotFoundException()
        cleanupPhotosAfterTransaction(removed = incident.photos)
    }

    @Transactional
    fun update(id: String, input: IncidentInput, deletePhotoIds: List<String>, photos: List<IncomingPhoto>): Incident {
        val existing = repository.findById(id) ?: throw IncidentNotFoundException()
        val valid = input.validated()
        val existingIds = existing.photos.map(IncidentPhoto::id).toSet()
        if (deletePhotoIds.size != deletePhotoIds.toSet().size || !existingIds.containsAll(deletePhotoIds)) {
            throw InvalidIncidentException("deletePhotoIds must belong to this incident and must not repeat")
        }
        val deletedIds = deletePhotoIds.toSet()
        val retained = existing.photos.filterNot { it.id in deletedIds }
        if (retained.size + photos.size !in 1..20) throw InvalidIncidentException("An incident needs 1 to 20 photos")

        val stored = savePhotos(photos)
        val removed = existing.photos.filter { it.id in deletedIds }
        cleanupPhotosAfterTransaction(added = stored, removed = removed)
        val now = Instant.now()
        val incident = existing.copy(
            title = valid.title,
            location = valid.location,
            description = valid.description,
            photos = retained + stored,
            updatedAt = now,
        )
        if (!repository.update(incident)) throw IncidentNotFoundException()
        return incident
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, query: String?, resolved: Boolean?): IncidentPage {
        if (page < 0 || size !in 1..100) {
            throw InvalidIncidentException("page must be nonnegative and size must be between 1 and 100")
        }
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedQuery != null && normalizedQuery.length > 200) {
            throw InvalidIncidentException("q must contain at most 200 characters")
        }
        return repository.findPage(page, size, normalizedQuery, resolved)
    }

    @Transactional
    fun setResolved(id: String, resolved: Boolean): Incident {
        repository.setResolved(id, resolved, Instant.now())
        return repository.findById(id) ?: throw IncidentNotFoundException()
    }

    @Transactional
    fun create(input: IncidentInput, photos: List<IncomingPhoto>): Incident {
        val valid = input.validated()
        if (photos.size !in 1..20) throw InvalidIncidentException("An incident needs 1 to 20 photos")
        val stored = savePhotos(photos)
        cleanupPhotosAfterTransaction(added = stored)
        val now = Instant.now()
        val incident = Incident(
            id = UUID.randomUUID().toString(),
            title = valid.title,
            location = valid.location,
            description = valid.description,
            photos = stored,
            createdAt = now,
            updatedAt = now,
        )
        repository.insert(incident)
        return incident
    }

    private fun savePhotos(photos: List<IncomingPhoto>): List<IncidentPhoto> {
        val stored = mutableListOf<IncidentPhoto>()
        try {
            photos.forEach { stored += IncidentPhoto(UUID.randomUUID().toString(), photoStore.save(it)) }
            return stored
        } catch (error: Exception) {
            stored.forEach { photoStore.delete(it.uri) }
            throw error
        }
    }

    private fun cleanupPhotosAfterTransaction(
        added: List<IncidentPhoto> = emptyList(),
        removed: List<IncidentPhoto> = emptyList(),
    ) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                val photos = if (status == TransactionSynchronization.STATUS_COMMITTED) removed else added
                photos.forEach { photoStore.delete(it.uri) }
            }
        })
    }
}
