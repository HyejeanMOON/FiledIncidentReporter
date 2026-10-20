package com.hyejeanmoon.fieldincidentreporter.data

import com.hyejeanmoon.fieldincidentreporter.BuildConfig
import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.repository.RetrofitIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun createIncidentRepository(): IncidentRepository {
    val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.INCIDENT_API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    return RetrofitIncidentRepository(retrofit.create(IncidentApi::class.java))
}
