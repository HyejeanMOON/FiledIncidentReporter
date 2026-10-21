package com.hyejeanmoon.fieldincidentreporter.data.remote

import com.google.gson.JsonParser
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentRepositoryException
import retrofit2.HttpException
import java.io.IOException

internal suspend fun <T> incidentRequest(block: suspend () -> T): T = try {
    block()
} catch (error: IOException) {
    throw IncidentRepositoryException(
        if (error is PhotoReadException) IncidentFailure.PHOTO else IncidentFailure.NETWORK,
        error,
    )
} catch (error: HttpException) {
    throw IncidentRepositoryException(error.toIncidentFailure(), error)
}

internal fun HttpException.toIncidentFailure(): IncidentFailure {
    val code = runCatching {
        response()?.errorBody()?.charStream()?.use { reader ->
            JsonParser.parseReader(reader).takeIf { it.isJsonObject }
                ?.asJsonObject?.get("code")?.takeIf { it.isJsonPrimitive }?.asString
        }
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
