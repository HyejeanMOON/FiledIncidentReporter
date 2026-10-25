package com.example.incidentreporter.incident.domain

/** Counts over every incident, regardless of any list filter. */
data class IncidentSummary(val open: Int, val resolved: Int) {
    val total get() = open + resolved
}
