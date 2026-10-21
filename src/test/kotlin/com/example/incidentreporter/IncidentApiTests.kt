package com.example.incidentreporter

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import com.jayway.jsonpath.JsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentApiTests @Autowired constructor(
    private val mvc: MockMvc,
    private val jdbc: JdbcClient,
) {
    @Test
    fun `delete removes incident and photos`() {
        val created = mvc.perform(
            post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Old","location":"Plant","description":"Details","photoUris":["content://old"]}"""),
        ).andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")

        mvc.perform(delete("/incidents/$id")).andExpect(status().isNoContent)
        val incidents = jdbc.sql("SELECT COUNT(*) FROM incidents WHERE id = :id")
            .param("id", id).query(Int::class.java).single()
        val photos = jdbc.sql("SELECT COUNT(*) FROM incident_photos WHERE incident_id = :id")
            .param("id", id).query(Int::class.java).single()
        assertEquals(0, incidents)
        assertEquals(0, photos)
        mvc.perform(delete("/incidents/$id")).andExpect(status().isNotFound)
    }

    @Test
    fun `update replaces incident fields and photos`() {
        val created = mvc.perform(
            post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Old","location":"Plant","description":"Old details","photoUris":["content://old"]}"""),
        ).andReturn().response.contentAsString
        val id = JsonPath.read<String>(created, "$.id")

        mvc.perform(
            put("/incidents/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"$id","title":"New","location":"Shop","description":"New details","photoUris":["content://new"]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("New"))
            .andExpect(jsonPath("$.photoUris[0]").value("content://new"))

        val stored = jdbc.sql("SELECT title FROM incidents WHERE id = :id")
            .param("id", id).query(String::class.java).single()
        assertEquals("New", stored)
        val photos = jdbc.sql("SELECT photo_uri FROM incident_photos WHERE incident_id = :id")
            .param("id", id).query(String::class.java).list()
        assertEquals(listOf("content://new"), photos)
    }

    @Test
    fun `update rejects missing incident`() {
        mvc.perform(
            put("/incidents/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"missing","title":"New","location":"Shop","description":"Details","photoUris":["content://new"]}"""),
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `list returns bounded pages with photo URIs`() {
        repeat(3) { index ->
            mvc.perform(
                post("/incidents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"Incident $index","location":"Plant","description":"Details","photoUris":["content://$index"]}"""),
            ).andExpect(status().isCreated)
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
        assertEquals(true, first.contains("content://"))
    }

    @Test
    fun `invalid page size is rejected`() {
        mvc.perform(get("/incidents?page=0&size=101")).andExpect(status().isBadRequest)
    }

    @Test
    fun `create incident persists its fields and photo order`() {
        mvc.perform(
            post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"  Broken pump  ","location":"Plant","description":"No pressure","photoUris":["content://one","content://two"]}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Broken pump"))
            .andExpect(jsonPath("$.photoUris[1]").value("content://two"))

        val saved = jdbc.sql("SELECT title FROM incidents").query(String::class.java).single()
        assertEquals("Broken pump", saved)
        val photos = jdbc.sql("SELECT photo_uri FROM incident_photos ORDER BY position")
            .query(String::class.java).list()
        assertEquals(listOf("content://one", "content://two"), photos)
    }

    @Test
    fun `invalid incident is rejected`() {
        mvc.perform(
            post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":" ","location":"Plant","description":"No pressure","photoUris":["content://one"]}"""),
        ).andExpect(status().isBadRequest)
    }
}
