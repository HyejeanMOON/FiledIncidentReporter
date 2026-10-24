package com.hyejeanmoon.fieldincidentreporter.domain.error

import kotlin.coroutines.cancellation.CancellationException

enum class IncidentFailure {
    /** No connection, a timeout, or the server is unreachable. */
    NETWORK_UNAVAILABLE,
    /** A selected photo's MIME type is not one the server accepts; detected before upload. */
    PHOTO_FORMAT_UNSUPPORTED,
    /** A selected photo could not be read from the device while uploading. */
    PHOTO_UNREADABLE,
    /** The server rejected the report's fields or photos (`INVALID_INCIDENT`). */
    INCIDENT_VALIDATION_FAILED,
    /** The server could not parse the request, usually an app/server version mismatch. */
    MALFORMED_REQUEST,
    /** The upload exceeds the server's size limit (HTTP 413). */
    UPLOAD_TOO_LARGE,
    /** The server failed while handling a valid request (HTTP 5xx). */
    SERVER_ERROR,
    /** The report no longer exists on the server. */
    INCIDENT_NOT_FOUND,
    /** An exception that was not translated into an [IncidentException]. */
    UNEXPECTED,
}

class IncidentException(
    val failure: IncidentFailure,
    cause: Throwable,
) : RuntimeException(cause)

fun Throwable.toIncidentFailure(): IncidentFailure =
    (this as? IncidentException)?.failure ?: IncidentFailure.UNEXPECTED

/** Like [runCatching], but lets coroutine cancellation propagate. */
suspend inline fun <T> runSuspendCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
