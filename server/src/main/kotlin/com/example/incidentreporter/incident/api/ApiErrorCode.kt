package com.example.incidentreporter.incident.api

enum class ApiErrorCode(val defaultDetail: String) {
    INCIDENT_NOT_FOUND("Incident not found"),
    INVALID_INCIDENT("Invalid incident"),
    INVALID_REQUEST("The request is invalid"),
    PAYLOAD_TOO_LARGE("The upload exceeds the allowed size"),
    RESOURCE_NOT_FOUND("Resource not found"),
    INTERNAL_ERROR("An unexpected server error occurred"),
}
