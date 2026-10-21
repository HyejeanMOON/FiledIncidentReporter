package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.application.IncidentService
import com.example.incidentreporter.incident.domain.Incident
import com.example.incidentreporter.incident.domain.IncidentInput
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class IncidentBody(
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
) {
    fun toInput() = IncidentInput(title, location, description, photoUris)
}

data class IncidentResponse(
    val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photoUris: List<String>,
)

fun Incident.toResponse() = IncidentResponse(id, title, location, description, photoUris)

@RestController
@RequestMapping("/incidents")
class IncidentController(private val service: IncidentService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody body: IncidentBody): IncidentResponse = service.create(body.toInput()).toResponse()
}
