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

internal class PhotoReadException(message: String, cause: IOException? = null) : IOException(message, cause)

internal fun photoPart(context: Context, value: String, index: Int): MultipartBody.Part {
    val uri = Uri.parse(value)
    val reportedType = context.contentResolver.getType(uri)
    val contentType = when (reportedType) {
        "image/jpg" -> "image/jpeg"
        "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/gif" -> reportedType
        else -> throw PhotoReadException("Unsupported photo format")
    }
    val mediaType = contentType.toMediaTypeOrNull()
        ?: throw PhotoReadException("Invalid photo content type")
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
