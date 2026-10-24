package com.hyejeanmoon.fieldincidentreporter.data.remote

import android.content.Context
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

// Some providers report the non-standard "image/jpg"; it is sent as MIME_JPEG.
private const val MIME_JPG = "image/jpg"
private const val MIME_JPEG = "image/jpeg"
private const val MIME_PNG = "image/png"
private const val MIME_WEBP = "image/webp"
private const val MIME_HEIC = "image/heic"
private const val MIME_HEIF = "image/heif"
private const val MIME_GIF = "image/gif"

internal class UnsupportedPhotoFormatException(message: String) : IOException(message)

internal class PhotoReadException(message: String, cause: IOException? = null) : IOException(message, cause)

internal fun photoPart(context: Context, value: String, index: Int): MultipartBody.Part {
    val uri = Uri.parse(value)
    val reportedType = context.contentResolver.getType(uri)
    val contentType = when (reportedType) {
        MIME_JPG -> MIME_JPEG
        MIME_JPEG, MIME_PNG, MIME_WEBP, MIME_HEIC, MIME_HEIF, MIME_GIF -> reportedType
        else -> throw UnsupportedPhotoFormatException("Unsupported photo format")
    }
    val mediaType = contentType.toMediaTypeOrNull()
        ?: throw UnsupportedPhotoFormatException("Invalid photo content type")
    val body = object : RequestBody() {
        override fun contentType(): MediaType = mediaType

        override fun writeTo(sink: BufferedSink) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Photo stream is unavailable")
                stream.use { sink.writeAll(it.source()) }
            } catch (error: IOException) {
                throw PhotoReadException("Could not read the selected photo", error)
            }
        }
    }
    return MultipartBody.Part.createFormData("photos", "incident-photo-$index", body)
}
