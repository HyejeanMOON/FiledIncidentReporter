package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentException
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException
import java.io.IOException

/** Runs an API call and translates transport and HTTP errors into [IncidentException]. */
internal suspend fun <T> incidentRequest(block: suspend () -> T): T = try {
    block()
} catch (error: IOException) {
    val failure = when (error) {
        is UnsupportedPhotoFormatException -> IncidentFailure.PHOTO_FORMAT_UNSUPPORTED
        is PhotoReadException -> IncidentFailure.PHOTO_UNREADABLE
        else -> IncidentFailure.NETWORK_UNAVAILABLE
    }
    throw IncidentException(failure, error)
} catch (error: HttpException) {
    throw IncidentException(error.toIncidentFailure(), error)
}

/** Reads the stable `code` of an RFC 9457 Problem Details body, falling back to the HTTP status. */
internal fun HttpException.toIncidentFailure(): IncidentFailure {
    val code = runCatching {
        val body = response()?.errorBody()?.string() ?: return@runCatching null
        ((Json.parseToJsonElement(body) as? JsonObject)?.get("code") as? JsonPrimitive)
            ?.takeIf { it.isString }?.content
    }.getOrNull()
    return failureForApiError(code, code())
}

/** The Problem Details `code` values the server sends. */
private object ApiErrorCode {
    const val INCIDENT_NOT_FOUND = "INCIDENT_NOT_FOUND"
    const val RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND"
    const val INVALID_INCIDENT = "INVALID_INCIDENT"
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}

internal fun failureForApiError(code: String?, status: Int): IncidentFailure = when (code) {
    ApiErrorCode.INCIDENT_NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND -> IncidentFailure.INCIDENT_NOT_FOUND
    ApiErrorCode.INVALID_INCIDENT -> IncidentFailure.INCIDENT_VALIDATION_FAILED
    ApiErrorCode.INVALID_REQUEST -> IncidentFailure.MALFORMED_REQUEST
    ApiErrorCode.PAYLOAD_TOO_LARGE -> IncidentFailure.UPLOAD_TOO_LARGE
    ApiErrorCode.INTERNAL_ERROR -> IncidentFailure.SERVER_ERROR
    else -> when (status) {
        400, 405, 415, 422 -> IncidentFailure.MALFORMED_REQUEST
        404 -> IncidentFailure.INCIDENT_NOT_FOUND
        413 -> IncidentFailure.UPLOAD_TOO_LARGE
        else -> IncidentFailure.SERVER_ERROR
    }
}
