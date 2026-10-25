package com.example.incidentreporter.incident.application

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import com.example.incidentreporter.incident.domain.IncidentNotFoundException
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.IncidentPhoto
import com.example.incidentreporter.incident.domain.IncidentPosition
import com.example.incidentreporter.incident.domain.IncidentRepository
import com.example.incidentreporter.incident.domain.IncomingPhoto
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.PhotoNotFoundException
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
    fun get(id: String): Incident = findExisting(id)

    @Transactional(readOnly = true)
    fun getPhoto(incidentId: String, photoId: String): StoredPhoto {
        val photo = findExisting(incidentId).photos.firstOrNull { it.id == photoId } ?: throw PhotoNotFoundException()
        return photoStore.load(photo.uri) ?: throw PhotoNotFoundException()
    }

    @Transactional
    fun delete(id: String) {
        val incident = findExisting(id)
        if (!repository.delete(id)) throw IncidentNotFoundException()
        cleanupPhotosAfterTransaction(removed = incident.photos)
    }

    @Transactional
    fun update(id: String, input: IncidentInput, deletePhotoIds: List<String>, photos: List<IncomingPhoto>): Incident {
        val existing = findExisting(id)
        val valid = input.validated()
        val deletedIds = deletePhotoIds.toSet()
        if (deletePhotoIds.size != deletedIds.size || !existing.photos.map(IncidentPhoto::id).containsAll(deletedIds)) {
            throw InvalidIncidentException("deletePhotoIds must belong to this incident and must not repeat")
        }
        val (removed, retained) = existing.photos.partition { it.id in deletedIds }
        Incident.requireValidPhotoCount(retained.size + photos.size)

        val stored = savePhotos(photos)
        cleanupPhotosAfterTransaction(added = stored, removed = removed)
        val incident = existing.copy(
            title = valid.title,
            location = valid.location,
            description = valid.description,
            photos = retained + stored,
            updatedAt = Instant.now(),
        )
        if (!repository.update(incident)) throw IncidentNotFoundException()
        return incident
    }

    /** A numbered [page] or a position to continue [after]; neither starts at the top. */
    @Transactional(readOnly = true)
    fun list(page: Int?, after: IncidentPosition?, size: Int, query: String?, resolved: Boolean?): IncidentPage {
        if (page != null && after != null) {
            throw InvalidIncidentException("page and cursor cannot be combined")
        }
        if ((page ?: 0) < 0 || size !in 1..MAX_PAGE_SIZE) {
            throw InvalidIncidentException("page must be nonnegative and size must be between 1 and $MAX_PAGE_SIZE")
        }
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedQuery != null && normalizedQuery.length > MAX_QUERY_LENGTH) {
            throw InvalidIncidentException("q must contain at most $MAX_QUERY_LENGTH characters")
        }
        return repository.findPage(after, (page ?: 0).toLong() * size, size, normalizedQuery, resolved)
    }

    @Transactional
    fun setResolved(id: String, resolved: Boolean): Incident {
        repository.setResolved(id, resolved, Instant.now())
        return findExisting(id)
    }

    @Transactional
    fun create(input: IncidentInput, photos: List<IncomingPhoto>): Incident {
        val valid = input.validated()
        Incident.requireValidPhotoCount(photos.size)
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

    private fun findExisting(id: String): Incident = repository.findById(id) ?: throw IncidentNotFoundException()

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

    /** Deletes [removed] files once the transaction commits, or the newly [added] files if it rolls back. */
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

    private companion object {
        const val MAX_PAGE_SIZE = 100
        const val MAX_QUERY_LENGTH = 200
    }
}
