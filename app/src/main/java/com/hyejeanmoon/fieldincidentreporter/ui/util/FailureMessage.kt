package com.hyejeanmoon.fieldincidentreporter.ui.util

import androidx.annotation.StringRes
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure

@StringRes
fun IncidentFailure.messageResId(): Int = when (this) {
    IncidentFailure.NETWORK_UNAVAILABLE -> R.string.error_network_unavailable
    IncidentFailure.PHOTO_FORMAT_UNSUPPORTED -> R.string.error_photo_format_unsupported
    IncidentFailure.PHOTO_UNREADABLE -> R.string.error_photo_unreadable
    IncidentFailure.INCIDENT_VALIDATION_FAILED -> R.string.error_incident_validation_failed
    IncidentFailure.MALFORMED_REQUEST -> R.string.error_malformed_request
    IncidentFailure.UPLOAD_TOO_LARGE -> R.string.error_upload_too_large
    IncidentFailure.SERVER_ERROR -> R.string.error_server_error
    IncidentFailure.INCIDENT_NOT_FOUND -> R.string.error_incident_not_found
    IncidentFailure.UNEXPECTED -> R.string.error_unexpected
}
