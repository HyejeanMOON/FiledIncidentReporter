package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentBody
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentDto
import com.hyejeanmoon.fieldincidentreporter.data.remote.dto.IncidentPageDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

internal interface IncidentApi {
    @GET("incidents/{id}")
    suspend fun getIncident(@Path("id") id: String): IncidentDto

    @GET("incidents")
    suspend fun getIncidents(@Query("page") page: Int, @Query("size") size: Int): IncidentPageDto

    @Multipart
    @POST("incidents")
    suspend fun create(@Part("incident") incident: IncidentBody, @Part photos: List<MultipartBody.Part>): IncidentDto

    @Multipart
    @PATCH("incidents/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Part("incident") incident: IncidentBody,
        @Part photos: List<MultipartBody.Part>,
    ): IncidentDto

    @DELETE("incidents/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
