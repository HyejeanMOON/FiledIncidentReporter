package com.hyejeanmoon.fieldincidentreporter.feature

import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentRepositoryException

internal fun Throwable.toIncidentFailure(): IncidentFailure =
    (this as? IncidentRepositoryException)?.failure ?: IncidentFailure.UNKNOWN
