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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import com.jayway.jsonpath.JsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentApiTests @Autowired constructor(
    private val mvc: MockMvc,
    private val jdbc: JdbcClient,
) {
    @Test
    fun `get incident by id includes its photos`() {
        val created = create("Find me").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        val photoId = JsonPath.read<String>(created, "$.photos[0].id")

        mvc.perform(get("/incidents/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Find me"))
            .andExpect(jsonPath("$.photos[0].id").value(photoId))
        mvc.perform(get("/incidents/missing")).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"))
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `delete removes incident and photos`() {
        val created = create("Old").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        val photoUri = jdbc.sql("SELECT photo_uri FROM incident_photos WHERE incident_id = :id")
            .param("id", id).query(String::class.java).single()

        mvc.perform(delete("/incidents/$id")).andExpect(status().isNoContent)
        val incidents = jdbc.sql("SELECT COUNT(*) FROM incidents WHERE id = :id")
            .param("id", id).query(Int::class.java).single()
        val photos = jdbc.sql("SELECT COUNT(*) FROM incident_photos WHERE incident_id = :id")
            .param("id", id).query(Int::class.java).single()
        assertEquals(0, incidents)
        assertEquals(0, photos)
        assertEquals(false, Files.exists(Path.of("target/test-photos").resolve(photoUri.removePrefix("/photos/"))))
        mvc.perform(delete("/incidents/$id")).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"))
    }

    @Test
    fun `update replaces incident fields and photos`() {
        val created = create("Old").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        val createdAt = Instant.parse(JsonPath.read(created, "$.createdAt"))
        val initialUpdatedAt = Instant.parse(JsonPath.read(created, "$.updatedAt"))
        assertEquals(createdAt, initialUpdatedAt)

        val updated = update(id, photos = listOf("new"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("New"))
            .andExpect(jsonPath("$.photos.length()").value(2))
            .andReturn().response.contentAsString
        val updatedAt = Instant.parse(JsonPath.read(updated, "$.updatedAt"))
        assertEquals(createdAt, Instant.parse(JsonPath.read(updated, "$.createdAt")))
        assertTrue(!updatedAt.isBefore(initialUpdatedAt))

        val stored = jdbc.sql("SELECT title, created_at, updated_at FROM incidents WHERE id = :id")
            .param("id", id)
            .query { rs, _ -> Triple(
                rs.getString("title"),
                rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                rs.getObject("updated_at", OffsetDateTime::class.java).toInstant(),
            ) }
            .single()
        assertEquals("New", stored.first)
        assertEquals(createdAt, stored.second)
        assertEquals(updatedAt, stored.third)
        val photos = jdbc.sql("SELECT photo_uri FROM incident_photos WHERE incident_id = :id")
            .param("id", id).query(String::class.java).list()
        assertEquals(2, photos.size)
        assertTrue(photos.all { requireNotNull(it).startsWith("/photos/") })
    }

    @Test
    fun `update rejects missing incident`() {
        update("missing", photos = listOf("new")).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"))
    }

    @Test
    fun `update deletes only photos owned by the incident`() {
        val created = create("Old").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        val photoId = JsonPath.read<String>(created, "$.photos[0].id")

        update(id, deleted = listOf(photoId), photos = listOf("replacement"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.photos[0].id").exists())
        update(id, deleted = listOf(photoId))
            .andExpect(status().isBadRequest)
        update(id, deleted = listOf("00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `update removes selected old photos and appends new files`() {
        val created = create("Mixed", listOf("A", "B", "C")).andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        val firstId = JsonPath.read<String>(created, "$.photos[0].id")
        val deletedId = JsonPath.read<String>(created, "$.photos[1].id")
        val thirdId = JsonPath.read<String>(created, "$.photos[2].id")
        val deletedUri = jdbc.sql("SELECT photo_uri FROM incident_photos WHERE incident_id = :incidentId AND photo_id = :photoId")
            .param("incidentId", id).param("photoId", deletedId).query(String::class.java).single()

        val updated = update(id, deleted = listOf(deletedId), photos = listOf("D", "E"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.photos.length()").value(4))
            .andReturn().response.contentAsString

        assertEquals(firstId, JsonPath.read<String>(updated, "$.photos[0].id"))
        assertEquals(thirdId, JsonPath.read<String>(updated, "$.photos[1].id"))
        val newIds = listOf(
            JsonPath.read<String>(updated, "$.photos[2].id"),
            JsonPath.read<String>(updated, "$.photos[3].id"),
        )
        assertTrue(newIds.none { it in setOf(firstId, deletedId, thirdId) })
        mvc.perform(get("/incidents/$id/photos/$deletedId"))
            .andExpect(status().isNotFound)
        assertEquals(false, Files.exists(Path.of("target/test-photos").resolve(deletedUri.removePrefix("/photos/"))))
        mvc.perform(delete("/incidents/$id")).andExpect(status().isNoContent)
    }

    @Test
    fun `list returns bounded pages with photos`() {
        repeat(3) { index ->
            create("Incident $index").andExpect(status().isCreated)
        }

        val first = mvc.perform(get("/incidents?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.items[0].createdAt").exists())
            .andExpect(jsonPath("$.items[0].updatedAt").exists())
            .andReturn().response.contentAsString
        mvc.perform(get("/incidents?page=1&size=2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].photos.length()").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
        assertTrue(first.contains("/thumbnail"))
        assertEquals(false, first.contains("base64,"))
    }

    @Test
    fun `cursor continues after its incident even once that incident is deleted`() {
        val ids = (0 until 3).map { JsonPath.read<String>(create("Cursor $it").andReturn().response.contentAsString, "$.id") }
        val first = mvc.perform(get("/incidents?size=1"))
            .andExpect(jsonPath("$.items[0].id").value(ids[2]))
            .andExpect(jsonPath("$.nextCursor").isString)
            .andReturn().response.contentAsString
        mvc.perform(delete("/incidents/${ids[2]}")).andExpect(status().isNoContent)

        mvc.perform(get("/incidents?size=2").param("cursor", nextCursor(first)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(ids[1]))
            .andExpect(jsonPath("$.items[1].id").value(ids[0]))
            .andExpect(jsonPath("$.page").value(null))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.nextCursor").value(null))
    }

    @Test
    fun `resolution can be set reopened and set again without losing the original transition time`() {
        val created = create("Resolution workflow").andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")
        assertEquals(false, JsonPath.read<Boolean>(created, "$.resolved"))
        assertEquals(null, JsonPath.read<String?>(created, "$.resolvedAt"))

        val first = resolution(id, true).andExpect(status().isOk)
            .andExpect(jsonPath("$.resolved").value(true))
            .andExpect(jsonPath("$.resolvedAt").exists())
            .andReturn().response.contentAsString
        val firstTime = JsonPath.read<String>(first, "$.resolvedAt")
        assertEquals(firstTime, JsonPath.read<String>(resolution(id, true).andReturn().response.contentAsString, "$.resolvedAt"))
        mvc.perform(get("/incidents/$id"))
            .andExpect(jsonPath("$.resolved").value(true))
            .andExpect(jsonPath("$.resolvedAt").value(firstTime))
        update(id).andExpect(status().isOk)
            .andExpect(jsonPath("$.resolvedAt").value(firstTime))

        resolution(id, false).andExpect(status().isOk)
            .andExpect(jsonPath("$.resolved").value(false))
            .andExpect(jsonPath("$.resolvedAt").value(null))
        resolution(id, true).andExpect(status().isOk)
            .andExpect(jsonPath("$.resolved").value(true))
            .andExpect(jsonPath("$.resolvedAt").exists())
        resolution("missing", true).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"))
    }

    @Test
    fun `list searches text fields and filters resolution before pagination`() {
        val titleId = JsonPath.read<String>(create("ZXQ-413 title").andReturn().response.contentAsString, "$.id")
        create("Other title", location = "ZXQ-413 location")
        create("Another title", description = "ZXQ-413 description")
        create("Percentage % marker")
        resolution(titleId, true).andExpect(status().isOk)

        mvc.perform(get("/incidents?q=zxq-413&resolved=true&page=0&size=1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(titleId))
            .andExpect(jsonPath("$.hasNext").value(false))
        mvc.perform(get("/incidents?q=ZXQ-413&resolved=false&page=0&size=1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.hasNext").value(true))
        mvc.perform(get("/incidents?q=ZXQ-413&resolved=false&page=1&size=1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
        mvc.perform(get("/incidents").param("q", "% marker"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
        mvc.perform(get("/incidents?q=${"x".repeat(201)}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INCIDENT"))
    }

    @Test
    fun `thumbnail endpoint returns resized compressed photo data`() {
        val image = BufferedImage(800, 1200, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.RED
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.color = Color.BLUE
        graphics.fillRect(0, image.height / 2, image.width, image.height / 2)
        graphics.dispose()
        val original = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        val metadata = incidentPart("""{"title":"Image","location":"Plant","description":"Details"}""")
        mvc.perform(multipart("/incidents").file(metadata).file(MockMultipartFile("photos", "image.png", "image/png", original)))
            .andExpect(status().isCreated)

        val response = mvc.perform(get("/incidents"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val thumbnailUrl = JsonPath.read<String>(response, "$.items[0].photos[0].thumbnailUrl")
        val compressed = mvc.perform(get(thumbnailUrl))
            .andExpect(status().isOk)
            .andReturn().response.contentAsByteArray
        assertTrue(compressed.size < original.size)
        val preview = ImageIO.read(compressed.inputStream())
        assertEquals(341, preview.width)
        assertEquals(512, preview.height)
        // Both halves of a portrait photo must survive the resize.
        assertTrue(Color(preview.getRGB(preview.width / 2, preview.height / 4)).red > 200)
        assertTrue(Color(preview.getRGB(preview.width / 2, preview.height * 3 / 4)).blue > 200)
    }

    @Test
    fun `invalid page size or cursor is rejected`() {
        create("Cursor source").andExpect(status().isCreated)
        create("Cursor target").andExpect(status().isCreated)
        val cursor = nextCursor(mvc.perform(get("/incidents?size=1")).andReturn().response.contentAsString)

        listOf("page=0&size=101", "page=-1", "cursor=not-a-cursor", "page=0&cursor=$cursor").forEach { query ->
            mvc.perform(get("/incidents?$query")).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("INVALID_INCIDENT"))
        }
    }

    @Test
    fun `create incident persists its fields and photo order`() {
        val created = create("  Broken pump  ", listOf("one", "two"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Broken pump"))
            .andExpect(jsonPath("$.photos.length()").value(2))
            .andReturn().response.contentAsString
        assertEquals(
            Instant.parse(JsonPath.read(created, "$.createdAt")),
            Instant.parse(JsonPath.read(created, "$.updatedAt")),
        )

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
        val id = JsonPath.read<String>(created, "$.id")
        val uri = JsonPath.read<String>(created, "$.photos[0].url")
        mvc.perform(get(uri))
            .andExpect(status().isOk)
            .andExpect(content().bytes("one".toByteArray()))
        mvc.perform(get("/incidents/$id/photos/missing")).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    @Test
    fun `invalid incident is rejected`() {
        create(" ").andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INCIDENT"))
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `failed second photo leaves no new file or incident`() {
        val photoDirectory = Path.of("target/test-photos")
        Files.createDirectories(photoDirectory)
        fun filenames(): Set<String> = Files.list(photoDirectory).use { paths ->
            paths.map { it.fileName.toString() }.toList().toSet()
        }
        val beforeFiles = filenames()
        val beforeIncidents = jdbc.sql("SELECT COUNT(*) FROM incidents").query(Int::class.java).single()
        val metadata = incidentPart("""{"title":"Invalid photos","location":"Plant","description":"Details"}""")

        mvc.perform(multipart("/incidents")
            .file(metadata)
            .file(jpegPart("first"))
            .file(MockMultipartFile("photos", "second.txt", "text/plain", "second".toByteArray())))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INCIDENT"))

        assertEquals(beforeFiles, filenames())
        assertEquals(beforeIncidents, jdbc.sql("SELECT COUNT(*) FROM incidents").query(Int::class.java).single())
    }

    @Test
    fun `malformed request has a stable error code`() {
        val metadata = incidentPart("{")
        mvc.perform(multipart("/incidents").file(metadata))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.status").value(400))
    }

    private fun create(
        title: String,
        photos: List<String> = listOf("one"),
        location: String = "Plant",
        description: String = "Details",
    ): ResultActions {
        val metadata = incidentPart("""{"title":"$title","location":"$location","description":"$description"}""")
        val request = multipart("/incidents").file(metadata)
        photos.forEach { photo ->
            request.file(jpegPart(photo))
        }
        return mvc.perform(request)
    }

    private fun nextCursor(page: String) = JsonPath.read<String>(page, "$.nextCursor")

    private fun resolution(id: String, resolved: Boolean): ResultActions = mvc.perform(
        patch("/incidents/$id/resolution")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"resolved":$resolved}"""),
    )

    private fun update(id: String, deleted: List<String> = emptyList(), photos: List<String> = emptyList()): ResultActions {
        val deletedJson = deleted.joinToString(",", "[", "]") { "\"$it\"" }
        val metadata = incidentPart("""{"title":"New","location":"Shop","description":"New details","deletePhotoIds":$deletedJson}""")
        val request = multipart("/incidents/$id").file(metadata).with { it.apply { method = "PATCH" } }
        photos.forEach { photo ->
            request.file(jpegPart(photo))
        }
        return mvc.perform(request)
    }

    private fun incidentPart(json: String) =
        MockMultipartFile("incident", "", MediaType.APPLICATION_JSON_VALUE, json.toByteArray())

    private fun jpegPart(content: String) = MockMultipartFile("photos", "$content.jpg", "image/jpeg", content.toByteArray())
}
