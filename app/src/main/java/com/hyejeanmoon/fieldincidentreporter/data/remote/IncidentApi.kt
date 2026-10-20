package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface IncidentApi {
    @GET("incidents")
    suspend fun getIncidents(): List<IncidentDto>

    @POST("incidents")
    suspend fun create(@Body incident: IncidentBody): IncidentDto

    @PUT("incidents/{id}")
    suspend fun update(@Path("id") id: String, @Body incident: IncidentDto): IncidentDto

    @DELETE("incidents/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
