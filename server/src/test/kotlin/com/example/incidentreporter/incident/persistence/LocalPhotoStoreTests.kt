package com.example.incidentreporter.incident.persistence

import com.example.incidentreporter.incident.domain.IncomingPhoto
import com.example.incidentreporter.incident.domain.InvalidIncidentException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LocalPhotoStoreTests {
    @TempDir
    lateinit var directory: Path

    private fun store() = LocalPhotoStore(PhotoStorageProperties(directory))

    private fun photo(contentType: String?, bytes: ByteArray = "data".toByteArray()) =
        IncomingPhoto(contentType, bytes.size.toLong()) { bytes.inputStream() }

    @Test
    fun `content types map to stored extensions and back`() {
        val store = store()
        mapOf(
            "image/jpeg" to "image/jpeg",
            "IMAGE/PNG" to "image/png",
            "image/webp" to "image/webp",
            "image/heic" to "image/heic",
            "image/heif" to "image/heic",
            "image/gif" to "image/gif",
        ).forEach { (uploaded, served) ->
            val uri = store.save(photo(uploaded))
            assertEquals(served, store.load(uri)?.contentType, uploaded)
        }
    }

    @Test
    fun `unsupported or empty photos are rejected`() {
        assertThrows<InvalidIncidentException> { store().save(photo("text/plain")) }
        assertThrows<InvalidIncidentException> { store().save(photo(null)) }
        assertThrows<InvalidIncidentException> { store().save(photo("image/png", ByteArray(0))) }
    }

    @Test
    fun `uris outside the photo namespace are ignored`() {
        assertNull(store().load("/etc/passwd"))
        assertNull(store().load("/photos/../secret.jpg"))
    }
}
