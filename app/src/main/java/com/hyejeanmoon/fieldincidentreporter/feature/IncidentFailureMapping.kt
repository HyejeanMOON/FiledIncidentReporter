package com.hyejeanmoon.fieldincidentreporter.feature

import androidx.annotation.StringRes
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentRepositoryException

internal fun Throwable.toIncidentFailure(): IncidentFailure =
    (this as? IncidentRepositoryException)?.failure ?: IncidentFailure.UNKNOWN

@StringRes
internal fun IncidentFailure.messageResId(): Int = when (this) {
    IncidentFailure.NETWORK -> R.string.error_network
    IncidentFailure.PHOTO -> R.string.error_photo
    IncidentFailure.INVALID_INCIDENT -> R.string.error_invalid_incident
    IncidentFailure.INVALID_REQUEST -> R.string.error_invalid_request
    IncidentFailure.PAYLOAD_TOO_LARGE -> R.string.error_payload_too_large
    IncidentFailure.SERVER -> R.string.error_server
    IncidentFailure.NOT_FOUND -> R.string.error_not_found
    IncidentFailure.UNKNOWN -> R.string.error_unknown
}
