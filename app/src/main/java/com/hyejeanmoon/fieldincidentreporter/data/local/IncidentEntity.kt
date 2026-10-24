package com.hyejeanmoon.fieldincidentreporter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentPhoto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/** A cached report. Times are epoch nanoseconds so that SQL sorts them exactly as the server does. */
@Entity(tableName = "incidents", indices = [Index("createdAt", "id")])
internal data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val location: String,
    val description: String,
    val photos: String,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long?,
)

@Serializable
private data class CachedPhoto(val id: String, val url: String, val thumbnailUrl: String)

internal fun Incident.toEntity() = IncidentEntity(
    id = id,
    title = title,
    location = location,
    description = description,
    photos = Json.encodeToString(photos.filterIsInstance<IncidentPhoto.Stored>().map { CachedPhoto(it.id, it.displayUri, it.thumbnailUri) }),
    createdAt = createdAt.toEpochNanos(),
    updatedAt = updatedAt.toEpochNanos(),
    resolvedAt = resolvedAt?.toEpochNanos(),
)

internal fun IncidentEntity.toIncident() = Incident(
    id = id,
    title = title,
    location = location,
    description = description,
    photos = Json.decodeFromString<List<CachedPhoto>>(photos).map { IncidentPhoto.Stored(it.id, it.url, it.thumbnailUrl) },
    createdAt = Instant.ofEpochSecond(0, createdAt),
    updatedAt = Instant.ofEpochSecond(0, updatedAt),
    resolvedAt = resolvedAt?.let { Instant.ofEpochSecond(0, it) },
)

private fun Instant.toEpochNanos(): Long = Math.addExact(Math.multiplyExact(epochSecond, 1_000_000_000L), nano.toLong())
