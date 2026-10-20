package com.hyejeanmoon.fieldincidentreporter.domain.error

class IncidentRepositoryException(
    val failure: IncidentFailure,
    cause: Throwable,
) : RuntimeException(cause)
