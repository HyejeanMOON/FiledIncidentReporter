package com.example.incidentreporter.incident.persistence

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentPhoto
import com.example.incidentreporter.incident.domain.IncidentRepository
import com.example.incidentreporter.incident.domain.IncidentPage
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class JdbcIncidentRepository(private val jdbc: JdbcClient) : IncidentRepository {
    override fun findById(id: String): Incident? {
        val incident = jdbc.sql("SELECT id, title, location, description FROM incidents WHERE id = :id")
            .param("id", id)
            .query { rs, _ ->
                Incident(rs.getString("id"), rs.getString("title"), rs.getString("location"), rs.getString("description"), emptyList())
            }
            .optional()
            .orElse(null) ?: return null
        val photos = jdbc.sql("SELECT photo_id, photo_uri FROM incident_photos WHERE incident_id = :id ORDER BY position")
            .param("id", id)
            .query { rs, _ -> IncidentPhoto(rs.getString("photo_id"), rs.getString("photo_uri")) }
            .list()
        return incident.copy(photos = photos)
    }

    override fun delete(id: String): Boolean =
        jdbc.sql("DELETE FROM incidents WHERE id = :id")
            .param("id", id)
            .update() > 0

    override fun update(incident: Incident): Boolean {
        val updated = jdbc.sql(
            "UPDATE incidents SET title = :title, location = :location, description = :description WHERE id = :id",
        )
            .param("id", incident.id)
            .param("title", incident.title)
            .param("location", incident.location)
            .param("description", incident.description)
            .update()
        if (updated == 0) return false

        jdbc.sql("DELETE FROM incident_photos WHERE incident_id = :id")
            .param("id", incident.id)
            .update()
        incident.photos.forEachIndexed { position, photo ->
            jdbc.sql("INSERT INTO incident_photos (incident_id, position, photo_id, photo_uri) VALUES (:id, :position, :photoId, :uri)")
                .param("id", incident.id)
                .param("position", position)
                .param("photoId", photo.id)
                .param("uri", photo.uri)
                .update()
        }
        return true
    }

    override fun findPage(page: Int, size: Int): IncidentPage {
        val rows = jdbc.sql(
            "SELECT id, title, location, description FROM incidents " +
                "ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset",
        )
            .param("limit", size + 1)
            .param("offset", page.toLong() * size)
            .query { rs, _ ->
                Incident(
                    id = rs.getString("id"),
                    title = rs.getString("title"),
                    location = rs.getString("location"),
                    description = rs.getString("description"),
                    photos = emptyList(),
                )
            }
            .list()
        val pageRows = rows.take(size)
        if (pageRows.isEmpty()) return IncidentPage(emptyList(), page, size, false)

        val photos = jdbc.sql(
            "SELECT incident_id, photo_id, photo_uri FROM incident_photos " +
                "WHERE incident_id IN (:ids) ORDER BY incident_id, position",
        )
            .param("ids", pageRows.map { it.id })
            .query { rs, _ -> rs.getString("incident_id") to IncidentPhoto(rs.getString("photo_id"), rs.getString("photo_uri")) }
            .list()
            .groupBy({ it.first }, { it.second })

        return IncidentPage(pageRows.map { it.copy(photos = photos[it.id].orEmpty()) }, page, size, rows.size > size)
    }

    override fun insert(incident: Incident) {
        jdbc.sql("INSERT INTO incidents (id, title, location, description) VALUES (:id, :title, :location, :description)")
            .param("id", incident.id)
            .param("title", incident.title)
            .param("location", incident.location)
            .param("description", incident.description)
            .update()

        incident.photos.forEachIndexed { position, photo ->
            jdbc.sql("INSERT INTO incident_photos (incident_id, position, photo_id, photo_uri) VALUES (:id, :position, :photoId, :uri)")
                .param("id", incident.id)
                .param("position", position)
                .param("photoId", photo.id)
                .param("uri", photo.uri)
                .update()
        }
    }
}
