package com.example.incidentreporter.incident.persistence

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentRepository
import com.example.incidentreporter.incident.domain.IncidentPage
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class JdbcIncidentRepository(private val jdbc: JdbcClient) : IncidentRepository {
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
                    photoUris = emptyList(),
                )
            }
            .list()
        val pageRows = rows.take(size)
        if (pageRows.isEmpty()) return IncidentPage(emptyList(), page, size, false)

        val photos = jdbc.sql(
            "SELECT incident_id, photo_uri FROM incident_photos " +
                "WHERE incident_id IN (:ids) ORDER BY incident_id, position",
        )
            .param("ids", pageRows.map { it.id })
            .query { rs, _ -> rs.getString("incident_id") to rs.getString("photo_uri") }
            .list()
            .groupBy({ it.first }, { it.second })

        return IncidentPage(pageRows.map { it.copy(photoUris = photos[it.id].orEmpty()) }, page, size, rows.size > size)
    }

    override fun insert(incident: Incident) {
        jdbc.sql("INSERT INTO incidents (id, title, location, description) VALUES (:id, :title, :location, :description)")
            .param("id", incident.id)
            .param("title", incident.title)
            .param("location", incident.location)
            .param("description", incident.description)
            .update()

        incident.photoUris.forEachIndexed { position, uri ->
            jdbc.sql("INSERT INTO incident_photos (incident_id, position, photo_uri) VALUES (:id, :position, :uri)")
                .param("id", incident.id)
                .param("position", position)
                .param("uri", uri)
                .update()
        }
    }
}
