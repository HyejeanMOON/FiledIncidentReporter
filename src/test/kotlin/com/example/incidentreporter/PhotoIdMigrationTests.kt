package com.example.incidentreporter

import org.h2.tools.RunScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.sql.DriverManager

class PhotoIdMigrationTests {
    @Test
    fun `existing photo path receives stable id`() {
        DriverManager.getConnection("jdbc:h2:mem:photo_migration;DB_CLOSE_DELAY=-1").use { connection ->
            listOf("V1__create_incidents.sql").forEach { migration ->
                val sql = javaClass.getResource("/db/migration/$migration")!!.readText()
                RunScript.execute(connection, StringReader(sql))
            }
            connection.createStatement().use { statement ->
                statement.executeUpdate("INSERT INTO incidents (id, title, location, description) VALUES ('incident-1', 'T', 'L', 'D')")
                statement.executeUpdate("INSERT INTO incident_photos (incident_id, position, photo_uri) VALUES ('incident-1', 0, '/photos/123e4567-e89b-12d3-a456-426614174000.jpg')")
            }
            val migration = javaClass.getResource("/db/migration/V2__add_photo_ids.sql")!!.readText()
            RunScript.execute(connection, StringReader(migration))
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT photo_id FROM incident_photos").use { result ->
                    result.next()
                    assertEquals("123e4567-e89b-12d3-a456-426614174000", result.getString(1))
                }
            }
        }
    }
}
