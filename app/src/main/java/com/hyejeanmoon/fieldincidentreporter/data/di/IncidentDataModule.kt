package com.hyejeanmoon.fieldincidentreporter.data.di

import com.hyejeanmoon.fieldincidentreporter.BuildConfig
import com.hyejeanmoon.fieldincidentreporter.data.remote.IncidentApi
import com.hyejeanmoon.fieldincidentreporter.data.repository.RetrofitIncidentRepository
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

/** Tolerates new server fields, and missing or null optional ones. */
internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class IncidentRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindIncidentRepository(repository: RetrofitIncidentRepository): IncidentRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object IncidentNetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.INCIDENT_API_BASE_URL)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideIncidentApi(retrofit: Retrofit): IncidentApi = retrofit.create(IncidentApi::class.java)
}
