package com.hyejeanmoon.fieldincidentreporter.data.remote

import android.content.Context
import androidx.core.net.toUri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSource
import okio.Source
import okio.source
import java.io.IOException
import java.io.InputStream

/** The photo types the server accepts. */
private val SupportedPhotoTypes = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/gif")

internal class UnsupportedPhotoFormatException(message: String) : IOException(message)

internal class PhotoReadException(message: String, cause: Exception? = null) : IOException(message, cause)

internal fun photoPart(context: Context, value: String, index: Int): MultipartBody.Part {
    val uri = value.toUri()
    val mediaType = photoContentType(context.contentResolver.getType(uri)).toMediaType()
    val body = PhotoRequestBody(mediaType) { context.contentResolver.openInputStream(uri) }
    return MultipartBody.Part.createFormData("photos", "incident-photo-$index", body)
}

/** Some providers report the non-standard "image/jpg", which is sent as "image/jpeg". */
internal fun photoContentType(reportedType: String?): String =
    (if (reportedType == "image/jpg") "image/jpeg" else reportedType)
        ?.takeIf { it in SupportedPhotoTypes }
        ?: throw UnsupportedPhotoFormatException("Unsupported photo format: $reportedType")

/**
 * Streams a photo from [openStream] each time the body is written. Only a failure to open or read
 * the photo becomes a [PhotoReadException]; a failure to send it is left as a network error.
 */
internal class PhotoRequestBody(
    private val mediaType: MediaType,
    private val openStream: () -> InputStream?,
) : RequestBody() {
    override fun contentType(): MediaType = mediaType

    override fun writeTo(sink: BufferedSink) {
        val stream = try {
            openStream()
        } catch (error: IOException) {
            throw PhotoReadException("Could not open the selected photo", error)
        } catch (error: SecurityException) {
            throw PhotoReadException("No longer allowed to read the selected photo", error)
        } ?: throw PhotoReadException("Photo stream is unavailable")
        stream.use { sink.writeAll(PhotoSource(it.source())) }
    }
}

/** Reads into the sink's buffer, so only errors from the photo itself pass through here. */
private class PhotoSource(delegate: Source) : ForwardingSource(delegate) {
    override fun read(sink: Buffer, byteCount: Long): Long = try {
        super.read(sink, byteCount)
    } catch (error: IOException) {
        throw PhotoReadException("Could not read the selected photo", error)
    }
}
