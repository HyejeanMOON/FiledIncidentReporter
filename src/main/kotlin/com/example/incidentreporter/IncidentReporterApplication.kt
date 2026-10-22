package com.example.incidentreporter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class IncidentReporterApplication

fun main(args: Array<String>) {
    runApplication<IncidentReporterApplication>(*args)
}
