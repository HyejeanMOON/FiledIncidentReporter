package com.hyejeanmoon.fieldincidentreporter.data.local

import android.content.Context
import androidx.core.content.FileProvider
import com.hyejeanmoon.fieldincidentreporter.domain.photo.CapturedPhotoStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/** Keeps camera photos in app-private storage, shared with the camera app through the FileProvider. */
internal class CapturedPhotoFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) : CapturedPhotoStore {
    // Must match the path in res/xml/file_paths.xml.
    private val directory get() = File(context.filesDir, "incident_photos")

    override fun newPhotoUri(): String {
        val file = File(directory.apply { mkdirs() }, "incident_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
    }

    override fun clear() {
        directory.deleteRecursively()
    }
}
