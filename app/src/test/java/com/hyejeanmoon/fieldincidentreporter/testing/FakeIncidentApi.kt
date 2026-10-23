package com.hyejeanmoon.fieldincidentreporter.testing

import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import okhttp3.MultipartBody
import retrofit2.Response

/** Serves [incidents], newest first, in pages the way the server does. */
internal class FakeIncidentApi(var incidents: List<IncidentDto>) : IncidentApi {
    var failure: Throwable? = null
    val requestedPages = mutableListOf<Int>()

    override suspend fun getIncidents(page: Int, size: Int): IncidentPageDto {
        requestedPages += page
        failure?.let { throw it }
        return IncidentPageDto(incidents.drop(page * size).take(size), page, size, (page + 1) * size < incidents.size)
    }

    override suspend fun getIncident(id: String): IncidentDto = error("Not used by these tests")

    override suspend fun create(incident: IncidentBody, photos: List<MultipartBody.Part>): IncidentDto =
        error("Not used by these tests")

    override suspend fun update(id: String, incident: IncidentBody, photos: List<MultipartBody.Part>): IncidentDto =
        error("Not used by these tests")

    override suspend fun delete(id: String): Response<Unit> = error("Not used by these tests")
}
