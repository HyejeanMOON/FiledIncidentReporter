package com.example.incidentreporter.incident.persistence

import com.example.incidentreporter.incident.domain.IncomingPhoto
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import com.example.incidentreporter.incident.domain.PhotoStore
import com.example.incidentreporter.incident.domain.StoredPhoto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@ConfigurationProperties("incident.photos")
data class PhotoStorageProperties(val directory: Path)

@Component
class LocalPhotoStore(properties: PhotoStorageProperties) : PhotoStore {
    private val root = properties.directory.toAbsolutePath().normalize()

    override fun save(photo: IncomingPhoto): String {
        val extension = EXTENSIONS_BY_CONTENT_TYPE[photo.contentType?.lowercase()]
            ?: throw InvalidIncidentException("Unsupported photo content type")
        if (photo.size !in 1..MAX_PHOTO_BYTES) throw InvalidIncidentException("Each photo must be 1 to 10 MB")

        Files.createDirectories(root)
        val filename = "${UUID.randomUUID()}.$extension"
        val destination = root.resolve(filename)
        try {
            photo.openStream().use { input -> Files.copy(input, destination) }
        } catch (error: Exception) {
            Files.deleteIfExists(destination)
            throw error
        }
        return "$URI_PREFIX$filename"
    }

    override fun delete(uri: String) {
        val path = resolve(uri) ?: return
        Files.deleteIfExists(path)
    }

    override fun load(uri: String): StoredPhoto? {
        val path = resolve(uri) ?: return null
        if (!Files.isRegularFile(path)) return null
        val contentType = CONTENT_TYPES_BY_EXTENSION[path.fileName.toString().substringAfterLast('.')] ?: return null
        return StoredPhoto(contentType, Files.size(path)) { Files.newInputStream(path) }
    }

    private fun resolve(uri: String): Path? {
        if (!uri.startsWith(URI_PREFIX)) return null
        val filename = uri.removePrefix(URI_PREFIX)
        return if (filename.matches(FILENAME_PATTERN)) root.resolve(filename) else null
    }

    private companion object {
        const val URI_PREFIX = "/photos/"
        const val MAX_PHOTO_BYTES = 10L * 1024 * 1024

        val CONTENT_TYPES_BY_EXTENSION = mapOf(
            "jpg" to "image/jpeg",
            "png" to "image/png",
            "webp" to "image/webp",
            "heic" to "image/heic",
            "gif" to "image/gif",
        )
        val EXTENSIONS_BY_CONTENT_TYPE =
            CONTENT_TYPES_BY_EXTENSION.entries.associate { (extension, type) -> type to extension } + ("image/heif" to "heic")
        val FILENAME_PATTERN = Regex("[0-9a-f-]{36}\\.(${CONTENT_TYPES_BY_EXTENSION.keys.joinToString("|")})")
    }
}
