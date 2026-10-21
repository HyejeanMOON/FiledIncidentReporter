package com.hyejeanmoon.fieldincidentreporter.data.remote

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
    val failure = if (error.code() == 404) IncidentFailure.NOT_FOUND else IncidentFailure.SERVER
    throw IncidentRepositoryException(failure, error)
}
