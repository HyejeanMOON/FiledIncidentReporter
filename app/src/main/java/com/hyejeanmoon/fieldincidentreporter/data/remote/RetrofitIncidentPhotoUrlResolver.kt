package com.hyejeanmoon.fieldincidentreporter.data.remote

import retrofit2.Retrofit
import javax.inject.Inject

internal class RetrofitIncidentPhotoUrlResolver @Inject constructor(
    private val retrofit: Retrofit,
) {
    fun resolve(photoPath: String): String =
        retrofit.baseUrl().resolve(photoPath)?.toString() ?: photoPath
}
