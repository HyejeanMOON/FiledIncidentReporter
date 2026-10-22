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
    throw IncidentException(
        if (error is PhotoReadException) IncidentFailure.PHOTO else IncidentFailure.NETWORK,
        error,
    )
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

internal fun failureForApiError(code: String?, status: Int): IncidentFailure = when (code) {
    "INCIDENT_NOT_FOUND", "RESOURCE_NOT_FOUND" -> IncidentFailure.NOT_FOUND
    "INVALID_INCIDENT" -> IncidentFailure.INVALID_INCIDENT
    "INVALID_REQUEST" -> IncidentFailure.INVALID_REQUEST
    "PAYLOAD_TOO_LARGE" -> IncidentFailure.PAYLOAD_TOO_LARGE
    "INTERNAL_ERROR" -> IncidentFailure.SERVER
    else -> when (status) {
        400, 405, 415, 422 -> IncidentFailure.INVALID_REQUEST
        404 -> IncidentFailure.NOT_FOUND
        413 -> IncidentFailure.PAYLOAD_TOO_LARGE
        else -> IncidentFailure.SERVER
    }
}
