package com.hyejeanmoon.fieldincidentreporter.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import okio.Sink
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class PhotoUploadTest {
    // Larger than an Okio segment, so writing it reaches the network sink.
    private val photo = ByteArray(64 * 1024) { it.toByte() }

    @Test
    fun `the photo is written to the request`() {
        val sink = Buffer()

        body { ByteArrayInputStream(photo) }.writeTo(sink)

        assertArrayEquals(photo, sink.readByteArray())
    }

    @Test
    fun `a photo that cannot be opened or read is a photo error`() {
        val failingRead = object : InputStream() {
            override fun read(): Int = throw IOException("Storage removed")
        }

        assertThrows(PhotoReadException::class.java) { body { throw FileNotFoundException() }.writeTo(Buffer()) }
        assertThrows(PhotoReadException::class.java) { body { throw SecurityException() }.writeTo(Buffer()) }
        assertThrows(PhotoReadException::class.java) { body { null }.writeTo(Buffer()) }
        assertThrows(PhotoReadException::class.java) { body { failingRead }.writeTo(Buffer()) }
    }

    @Test
    fun `a connection lost while sending the photo stays a network error`() {
        val offline = object : Sink {
            override fun write(source: Buffer, byteCount: Long) = throw IOException("Connection reset")
            override fun flush() = Unit
            override fun timeout() = Timeout.NONE
            override fun close() = Unit
        }

        val error = assertThrows(IOException::class.java) { body { ByteArrayInputStream(photo) }.writeTo(offline.buffer()) }

        assertFalse(error is PhotoReadException)
    }

    @Test
    fun `only the photo types the server accepts are uploaded`() {
        assertEquals("image/jpeg", photoContentType("image/jpg"))
        listOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/gif").forEach { type ->
            assertEquals(type, photoContentType(type))
        }
        listOf("image/bmp", "video/mp4", null).forEach { type ->
            assertThrows(UnsupportedPhotoFormatException::class.java) { photoContentType(type) }
        }
    }

    private fun body(openStream: () -> InputStream?) = PhotoRequestBody("image/jpeg".toMediaType(), openStream)
}
