package com.example.incidentreporter.incident.domain

class InvalidIncidentException(message: String) : RuntimeException(message)
class IncidentNotFoundException : RuntimeException("Incident not found")
class PhotoNotFoundException : RuntimeException("Photo not found")
