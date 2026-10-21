package com.example.incidentreporter.incident.domain

import java.io.InputStream

data class IncomingPhoto(
    val contentType: String?,
    val size: Long,
    val openStream: () -> InputStream,
)

interface PhotoStore {
    fun save(photo: IncomingPhoto): String
    fun delete(uri: String)
    fun load(uri: String): StoredPhoto?
}

data class StoredPhoto(
    val contentType: String,
    val size: Long,
    val openStream: () -> InputStream,
)
