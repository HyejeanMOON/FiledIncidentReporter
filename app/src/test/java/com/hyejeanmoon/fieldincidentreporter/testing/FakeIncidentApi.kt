package com.hyejeanmoon.fieldincidentreporter.testing

import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import okhttp3.MultipartBody
import retrofit2.Response
import java.time.Instant

/** Serves [incidents], newest first, in keyset pages the way the server does. */
internal class FakeIncidentApi(var incidents: List<IncidentDto>) : IncidentApi {
    var failure: Throwable? = null
    val requestedCursors = mutableListOf<String?>()

    override suspend fun getIncidents(cursor: String?, size: Int): IncidentPageDto {
        requestedCursors += cursor
        failure?.let { throw it }
        val rest = if (cursor == null) incidents else incidents.filter { it.comesAfter(cursor) }
        val items = rest.take(size)
        return IncidentPageDto(items, items.lastOrNull()?.takeIf { rest.size > size }?.let(::cursorAfter))
    }

    override suspend fun getIncident(id: String): IncidentDto = error("Not used by these tests")

    override suspend fun create(incident: IncidentBody, photos: List<MultipartBody.Part>): IncidentDto =
        error("Not used by these tests")

    override suspend fun update(id: String, incident: IncidentBody, photos: List<MultipartBody.Part>): IncidentDto =
        error("Not used by these tests")

    override suspend fun delete(id: String): Response<Unit> = error("Not used by these tests")
}

/** The cursor of a page that ends with [incident]: its position, which stays valid if the report is deleted. */
internal fun cursorAfter(incident: IncidentDto) = "${incident.createdAt}|${incident.id}"

private fun IncidentDto.comesAfter(cursor: String): Boolean {
    val (createdAt, id) = cursor.split('|')
    val order = Instant.parse(this.createdAt).compareTo(Instant.parse(createdAt))
    return order < 0 || order == 0 && this.id < id
}
