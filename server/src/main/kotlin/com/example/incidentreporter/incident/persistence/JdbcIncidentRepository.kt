package com.example.incidentreporter.incident.persistence

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentPage
import com.example.incidentreporter.incident.domain.IncidentPhoto
import com.example.incidentreporter.incident.domain.IncidentPosition
import com.example.incidentreporter.incident.domain.IncidentRepository
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale

@Repository
class JdbcIncidentRepository(private val jdbc: JdbcClient) : IncidentRepository {
    override fun findById(id: String): Incident? {
        val incident = jdbc.sql("$SELECT_INCIDENTS WHERE id = :id")
            .param("id", id)
            .query { rs, _ -> mapIncident(rs) }
            .optional()
            .orElse(null) ?: return null
        return incident.copy(photos = findPhotos(listOf(id))[id].orEmpty())
    }

    override fun delete(id: String): Boolean =
        jdbc.sql("DELETE FROM incidents WHERE id = :id")
            .param("id", id)
            .update() > 0

    override fun update(incident: Incident): Boolean {
        val updated = jdbc.sql(
            "UPDATE incidents SET title = :title, location = :location, description = :description, updated_at = :updatedAt WHERE id = :id",
        )
            .param("id", incident.id)
            .param("title", incident.title)
            .param("location", incident.location)
            .param("description", incident.description)
            .param("updatedAt", incident.updatedAt.toUtc())
            .update()
        if (updated == 0) return false

        jdbc.sql("DELETE FROM incident_photos WHERE incident_id = :id")
            .param("id", incident.id)
            .update()
        insertPhotos(incident)
        return true
    }

    override fun setResolved(id: String, resolved: Boolean, at: Instant) {
        val sql = if (resolved) {
            "UPDATE incidents SET resolved_at = :at, updated_at = :at WHERE id = :id AND resolved_at IS NULL"
        } else {
            "UPDATE incidents SET resolved_at = NULL, updated_at = :at WHERE id = :id AND resolved_at IS NOT NULL"
        }
        jdbc.sql(sql)
            .param("id", id)
            .param("at", at.toUtc())
            .update()
    }

    override fun findPage(after: IncidentPosition?, offset: Long, size: Int, query: String?, resolved: Boolean?): IncidentPage {
        val filters = mutableListOf<String>()
        val params = mutableMapOf<String, Any>("limit" to size + 1, "offset" to offset)
        if (after != null) {
            // Continues right after the position, so incidents added or deleted before it do not shift the page.
            filters += "(created_at < :afterCreatedAt OR (created_at = :afterCreatedAt AND id < :afterId))"
            params["afterCreatedAt"] = after.createdAt.toUtc()
            params["afterId"] = after.id
        }
        if (query != null) {
            val escaped = query.lowercase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            filters += "(LOWER(title) LIKE :pattern ESCAPE '!' OR LOWER(location) LIKE :pattern ESCAPE '!' OR LOWER(description) LIKE :pattern ESCAPE '!')"
            params["pattern"] = "%$escaped%"
        }
        if (resolved != null) {
            filters += if (resolved) "resolved_at IS NOT NULL" else "resolved_at IS NULL"
        }
        val where = if (filters.isEmpty()) "" else "WHERE ${filters.joinToString(" AND ")} "
        val rows = jdbc.sql("$SELECT_INCIDENTS ${where}ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset")
            .params(params)
            .query { rs, _ -> mapIncident(rs) }
            .list()
        val pageRows = rows.take(size)
        if (pageRows.isEmpty()) return IncidentPage(emptyList(), null)

        val photos = findPhotos(pageRows.map(Incident::id))
        val next = pageRows.last().takeIf { rows.size > size }?.let { IncidentPosition(it.createdAt, it.id) }
        return IncidentPage(pageRows.map { it.copy(photos = photos[it.id].orEmpty()) }, next)
    }

    override fun insert(incident: Incident) {
        jdbc.sql("INSERT INTO incidents (id, title, location, description, created_at, updated_at) VALUES (:id, :title, :location, :description, :createdAt, :updatedAt)")
            .param("id", incident.id)
            .param("title", incident.title)
            .param("location", incident.location)
            .param("description", incident.description)
            .param("createdAt", incident.createdAt.toUtc())
            .param("updatedAt", incident.updatedAt.toUtc())
            .update()

        insertPhotos(incident)
    }

    /** Loads the photos of the given incidents, keyed by incident ID and kept in their stored order. */
    private fun findPhotos(incidentIds: List<String>): Map<String, List<IncidentPhoto>> =
        jdbc.sql(
            "SELECT incident_id, photo_id, photo_uri FROM incident_photos " +
                "WHERE incident_id IN (:ids) ORDER BY incident_id, position",
        )
            .param("ids", incidentIds)
            .query { rs, _ -> rs.getString("incident_id") to IncidentPhoto(rs.getString("photo_id"), rs.getString("photo_uri")) }
            .list()
            .groupBy({ it.first }, { it.second })

    private fun insertPhotos(incident: Incident) {
        incident.photos.forEachIndexed { position, photo ->
            jdbc.sql("INSERT INTO incident_photos (incident_id, position, photo_id, photo_uri) VALUES (:id, :position, :photoId, :uri)")
                .param("id", incident.id)
                .param("position", position)
                .param("photoId", photo.id)
                .param("uri", photo.uri)
                .update()
        }
    }

    private fun mapIncident(rs: ResultSet) = Incident(
        id = rs.getString("id"),
        title = rs.getString("title"),
        location = rs.getString("location"),
        description = rs.getString("description"),
        photos = emptyList(),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
        updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java).toInstant(),
        resolvedAt = rs.getObject("resolved_at", OffsetDateTime::class.java)?.toInstant(),
    )

    private fun Instant.toUtc(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

    private companion object {
        const val SELECT_INCIDENTS =
            "SELECT id, title, location, description, created_at, updated_at, resolved_at FROM incidents"
    }
}
