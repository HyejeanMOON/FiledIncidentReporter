package com.hyejeanmoon.fieldincidentreporter.data.local

import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import com.hyejeanmoon.fieldincidentreporter.testing.testIncident
import com.hyejeanmoon.fieldincidentreporter.testing.testTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class IncidentCacheTest {
    @Test
    fun `unchanged page writes nothing`() {
        val page = listOf(testIncident("a").toEntity(), testIncident("b").toEntity())

        assertEquals(PageChanges(emptyList(), emptyList()), pageChanges(cached = page, page = page))
    }

    @Test
    fun `page upserts changed and new reports and deletes missing ones`() {
        val cached = listOf(testIncident("a"), testIncident("b"), testIncident("c")).map { it.toEntity() }
        val page = listOf(testIncident("b", "Updated").toEntity(), testIncident("c").toEntity(), testIncident("d").toEntity())

        assertEquals(PageChanges(upserts = listOf(page[0], page[2]), deletedIds = listOf("a")), pageChanges(cached, page))
    }

    @Test
    fun `older server copy does not replace a newer cached one`() {
        val saved = testIncident(title = "Saved").copy(updatedAt = testTime.plusSeconds(60)).toEntity()
        val stale = testIncident(title = "Stale").toEntity()

        assertEquals(PageChanges(emptyList(), emptyList()), pageChanges(cached = listOf(saved), page = listOf(stale)))
    }

    @Test
    fun `caching keeps photos and sub-millisecond times`() {
        val incident = testIncident().copy(
            photos = listOf(IncidentPhoto.Stored("photo-1", "http://example.test/photo-1")),
            createdAt = Instant.parse("2026-10-20T09:00:00.123456Z"),
            updatedAt = Instant.parse("2026-10-20T09:00:00.123457Z"),
        )

        assertEquals(incident, incident.toEntity().toIncident())
    }
}
