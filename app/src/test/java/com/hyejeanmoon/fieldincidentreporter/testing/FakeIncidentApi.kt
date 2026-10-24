package com.hyejeanmoon.fieldincidentreporter.testing

import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant

/** Serves [incidents], newest first, in keyset pages the way the server does. */
internal class FakeIncidentApi(var incidents: List<IncidentDto>) : IncidentApi {
    var failure: Throwable? = null
    val requestedCursors = mutableListOf<String?>()
    /** The report id (null for a create), body, and number of uploaded photos of each write. */
    val writes = mutableListOf<Triple<String?, IncidentBody, Int>>()

    override suspend fun getIncidents(cursor: String?, size: Int): IncidentPageDto {
        requestedCursors += cursor
        failure?.let { throw it }
        val rest = if (cursor == null) incidents else incidents.filter { it.comesAfter(cursor) }
        val items = rest.take(size)
        return IncidentPageDto(items, items.lastOrNull()?.takeIf { rest.size > size }?.let(::cursorAfter))
    }

    override suspend fun getIncident(id: String): IncidentDto {
        failure?.let { throw it }
        return incidents.find { it.id == id } ?: throw HttpException(Response.error<IncidentDto>(404, "".toResponseBody()))
    }

    override suspend fun create(incident: IncidentBody, photos: List<MultipartBody.Part>): IncidentDto {
        writes += Triple(null, incident, photos.size)
        failure?.let { throw it }
        val time = Instant.parse(incidents.firstOrNull()?.createdAt ?: testTime.toString()).plusSeconds(60).toString()
        val created = IncidentDto(
            "incident-new", incident.title, incident.location, incident.description,
            createdAt = time, updatedAt = time,
        )
        incidents = listOf(created) + incidents
        return created
    }

    override suspend fun update(id: String, incident: IncidentBody, photos: List<MultipartBody.Part>): IncidentDto {
        writes += Triple(id, incident, photos.size)
        failure?.let { throw it }
        val old = incidents.find { it.id == id } ?: throw HttpException(Response.error<IncidentDto>(404, "".toResponseBody()))
        val updated = old.copy(
            title = incident.title,
            location = incident.location,
            description = incident.description,
            photos = old.photos.filterNot { it.id in incident.deletePhotoIds.orEmpty() },
            updatedAt = Instant.parse(old.updatedAt).plusSeconds(60).toString(),
        )
        incidents = incidents.map { if (it.id == id) updated else it }
        return updated
    }

    override suspend fun delete(id: String) {
        failure?.let { throw it }
        if (incidents.none { it.id == id }) throw HttpException(Response.error<Unit>(404, "".toResponseBody()))
        incidents = incidents.filterNot { it.id == id }
    }
}

/** The cursor of a page that ends with [incident]: its position, which stays valid if the report is deleted. */
internal fun cursorAfter(incident: IncidentDto) = "${incident.createdAt}|${incident.id}"

private fun IncidentDto.comesAfter(cursor: String): Boolean {
    val (createdAt, id) = cursor.split('|')
    val order = Instant.parse(this.createdAt).compareTo(Instant.parse(createdAt))
    return order < 0 || order == 0 && this.id < id
}
