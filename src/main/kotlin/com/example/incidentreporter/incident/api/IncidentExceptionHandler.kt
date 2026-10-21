package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.IncidentNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class IncidentExceptionHandler {
    @ExceptionHandler(IncidentNotFoundException::class)
    fun notFound(error: IncidentNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (error.message ?: "Incident not found")))

    @ExceptionHandler(InvalidIncidentException::class)
    fun invalid(error: InvalidIncidentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to (error.message ?: "Invalid incident")))
}
