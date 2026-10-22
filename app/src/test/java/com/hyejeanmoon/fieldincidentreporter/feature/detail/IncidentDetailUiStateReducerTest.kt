package com.hyejeanmoon.fieldincidentreporter.feature.detail

import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentDetailUiStateReducerTest {
    @Test
    fun `late response for previous incident cannot replace current detail`() {
        val loadingFirst = IncidentDetailUiStateReducer.reduce(
            IncidentDetailUiState(),
            IncidentDetailStateChange.LoadStarted("first"),
        )
        val loadingSecond = IncidentDetailUiStateReducer.reduce(
            loadingFirst,
            IncidentDetailStateChange.LoadStarted("second"),
        )
        val stale = IncidentDetailUiStateReducer.reduce(
            loadingSecond,
            IncidentDetailStateChange.LoadSucceeded("first", Incident("first", "Old", "A", "Old")),
        )
        assertEquals(null, stale.incident)

        val loaded = IncidentDetailUiStateReducer.reduce(
            stale,
            IncidentDetailStateChange.LoadSucceeded(
                "second",
                Incident(
                    "second", "New", "B", "New",
                    listOf(IncidentPhoto.Stored("/photos/2.jpg", "http://example.test/photos/2.jpg")),
                ),
            ),
        )
        assertEquals("second", loaded.incident?.id)
        assertEquals(listOf("http://example.test/photos/2.jpg"), loaded.incident?.photos?.map { it.displayUri })
    }
}
