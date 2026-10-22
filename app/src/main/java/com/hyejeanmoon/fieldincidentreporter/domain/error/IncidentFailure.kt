package com.hyejeanmoon.fieldincidentreporter.domain.error

import kotlin.coroutines.cancellation.CancellationException

enum class IncidentFailure {
    NETWORK,
    PHOTO,
    INVALID_INCIDENT,
    INVALID_REQUEST,
    PAYLOAD_TOO_LARGE,
    SERVER,
    NOT_FOUND,
    UNKNOWN,
}

class IncidentException(
    val failure: IncidentFailure,
    cause: Throwable,
) : RuntimeException(cause)

fun Throwable.toIncidentFailure(): IncidentFailure =
    (this as? IncidentException)?.failure ?: IncidentFailure.UNKNOWN

/** Like [runCatching], but lets coroutine cancellation propagate. */
suspend inline fun <T> runSuspendCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
