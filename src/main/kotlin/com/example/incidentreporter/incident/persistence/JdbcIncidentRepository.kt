package com.example.incidentreporter.incident.persistence

import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentRepository
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class JdbcIncidentRepository(private val jdbc: JdbcClient) : IncidentRepository {
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
