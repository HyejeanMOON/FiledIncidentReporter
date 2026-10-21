package com.example.incidentreporter.incident.persistence

import com.example.incidentreporter.incident.domain.IncomingPhoto
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.PhotoStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Component
class LocalPhotoStore(@Value("\${incident.photos.directory}") directory: String) : PhotoStore {
    private val root = Path.of(directory).toAbsolutePath().normalize()

    override fun save(photo: IncomingPhoto): String {
        val extension = when (photo.contentType?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            "image/gif" -> "gif"
            else -> throw InvalidIncidentException("Unsupported photo content type")
        }
        if (photo.size !in 1..10_485_760) throw InvalidIncidentException("Each photo must be 1 to 10 MB")

        Files.createDirectories(root)
        val filename = "${UUID.randomUUID()}.$extension"
        val destination = root.resolve(filename)
        try {
            photo.openStream().use { input -> Files.copy(input, destination) }
        } catch (error: Exception) {
            Files.deleteIfExists(destination)
            throw error
        }
        return "/photos/$filename"
    }

    override fun delete(uri: String) {
        val filename = uri.removePrefix("/photos/")
        if (uri.startsWith("/photos/") && filename.matches(Regex("[0-9a-f-]{36}\\.(jpg|png|webp|heic|gif)"))) {
            Files.deleteIfExists(root.resolve(filename))
        }
    }
}
