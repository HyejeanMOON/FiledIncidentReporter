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
