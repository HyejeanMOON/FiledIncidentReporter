package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentUpdateBody
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface IncidentApi {
    @GET("incidents")
    suspend fun getIncidents(): List<IncidentDto>

    @Multipart
    @POST("incidents")
    suspend fun create(@Part("incident") incident: IncidentBody, @Part photos: List<MultipartBody.Part>): IncidentDto

    @Multipart
    @PUT("incidents/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Part("incident") incident: IncidentUpdateBody,
        @Part photos: List<MultipartBody.Part>,
    ): IncidentDto

    @DELETE("incidents/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
