package com.example.incidentreporter

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import com.jayway.jsonpath.JsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentApiTests @Autowired constructor(
    private val mvc: MockMvc,
    private val jdbc: JdbcClient,
) {
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `delete removes incident and photos`() {
        val created = create("Old").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        val photoUri = JsonPath.read<String>(created, "$.photoUris[0]")

        mvc.perform(delete("/incidents/$id")).andExpect(status().isNoContent)
        val incidents = jdbc.sql("SELECT COUNT(*) FROM incidents WHERE id = :id")
            .param("id", id).query(Int::class.java).single()
        val photos = jdbc.sql("SELECT COUNT(*) FROM incident_photos WHERE incident_id = :id")
            .param("id", id).query(Int::class.java).single()
        assertEquals(0, incidents)
        assertEquals(0, photos)
        assertEquals(false, Files.exists(Path.of("target/test-photos").resolve(photoUri.removePrefix("/photos/"))))
        mvc.perform(delete("/incidents/$id")).andExpect(status().isNotFound)
    }

    @Test
    fun `update replaces incident fields and photos`() {
        val created = create("Old").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")

        update(id, photos = listOf("new"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("New"))
            .andExpect(jsonPath("$.photoUris.length()").value(1))

        val stored = jdbc.sql("SELECT title FROM incidents WHERE id = :id")
            .param("id", id).query(String::class.java).single()
        assertEquals("New", stored)
        val photos = jdbc.sql("SELECT photo_uri FROM incident_photos WHERE incident_id = :id")
            .param("id", id).query(String::class.java).list()
        assertEquals(1, photos.size)
        assertTrue(requireNotNull(photos.single()).startsWith("/photos/"))
    }

    @Test
    fun `update rejects missing incident`() {
        update("missing", photos = listOf("new")).andExpect(status().isNotFound)
    }

    @Test
    fun `update retains only photos owned by the incident`() {
        val created = create("Old").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        val photoUri = JsonPath.read<String>(created, "$.photoUris[0]")

        update(id, retained = listOf(photoUri))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.photoUris[0]").value(photoUri))
        update(id, retained = listOf("/photos/00000000-0000-0000-0000-000000000000.jpg"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `list returns bounded pages with photo URIs`() {
        repeat(3) { index ->
            create("Incident $index").andExpect(status().isCreated)
        }

        val first = mvc.perform(get("/incidents?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andReturn().response.contentAsString
        mvc.perform(get("/incidents?page=1&size=2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].photoUris.length()").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
        assertTrue(first.contains("/photos/"))
    }

    @Test
    fun `invalid page size is rejected`() {
        mvc.perform(get("/incidents?page=0&size=101")).andExpect(status().isBadRequest)
    }

    @Test
    fun `create incident persists its fields and photo order`() {
        create("  Broken pump  ", listOf("one", "two"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Broken pump"))
            .andExpect(jsonPath("$.photoUris.length()").value(2))

        val saved = jdbc.sql("SELECT title FROM incidents").query(String::class.java).single()
        assertEquals("Broken pump", saved)
        val photos = jdbc.sql("SELECT photo_uri FROM incident_photos ORDER BY position")
            .query(String::class.java).list()
        assertEquals(2, photos.size)
        photos.forEachIndexed { index, savedUri ->
            val uri = requireNotNull(savedUri)
            assertTrue(uri.startsWith("/photos/"))
            val path = Path.of("target/test-photos").resolve(uri.removePrefix("/photos/"))
            assertEquals(listOf("one", "two")[index], Files.readString(path))
        }
    }

    @Test
    fun `stored photo can be downloaded`() {
        val created = create("Photo test").andReturn().response.contentAsString
        val uri = JsonPath.read<String>(created, "$.photoUris[0]")
        mvc.perform(get(uri))
            .andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes("one".toByteArray()))
        mvc.perform(get("/photos/missing.jpg")).andExpect(status().isNotFound)
    }

    @Test
    fun `invalid incident is rejected`() {
        create(" ").andExpect(status().isBadRequest)
    }

    private fun create(title: String, photos: List<String> = listOf("one")): ResultActions {
        val metadata = MockMultipartFile(
            "incident", "", MediaType.APPLICATION_JSON_VALUE,
            """{"title":"$title","location":"Plant","description":"Details"}""".toByteArray(),
        )
        val request = multipart("/incidents").file(metadata)
        photos.forEach { photo ->
            request.file(MockMultipartFile("photos", "$photo.jpg", "image/jpeg", photo.toByteArray()))
        }
        return mvc.perform(request)
    }

    private fun update(id: String, retained: List<String> = emptyList(), photos: List<String> = emptyList()): ResultActions {
        val retainedJson = retained.joinToString(",", "[", "]") { "\"$it\"" }
        val metadata = MockMultipartFile(
            "incident", "", MediaType.APPLICATION_JSON_VALUE,
            """{"id":"$id","title":"New","location":"Shop","description":"New details","retainedPhotoUris":$retainedJson}""".toByteArray(),
        )
        val request = multipart("/incidents/$id").file(metadata).with { it.apply { method = "PUT" } }
        photos.forEach { photo ->
            request.file(MockMultipartFile("photos", "$photo.jpg", "image/jpeg", photo.toByteArray()))
        }
        return mvc.perform(request)
    }
}
