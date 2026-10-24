package com.hyejeanmoon.fieldincidentreporter.feature.form

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_PHOTOS

internal class PhotoInput(
    val choosePhotos: () -> Unit,
    val takePhoto: () -> Unit,
)

/** Wires the system photo picker and camera to [onPhotosSelected] and [onCancelled]; [newPhotoUri] is where the camera writes. */
@Composable
internal fun rememberPhotoInput(
    newPhotoUri: () -> String,
    onPhotosSelected: (List<String>) -> Unit,
    onCancelled: () -> Unit,
): PhotoInput {
    val context = LocalContext.current
    var pendingPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_INCIDENT_PHOTOS),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult onCancelled()
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Picker fallbacks may provide a temporary read grant only.
            }
        }
        onPhotosSelected(uris.map(Uri::toString))
    }
    /** Deletes the empty file made for a photo that was not taken. */
    fun discardPhoto(photoUri: Uri) {
        pendingPhotoUri = null
        context.contentResolver.delete(photoUri, null, null)
        onCancelled()
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val photoUri = pendingPhotoUri ?: return@rememberLauncherForActivityResult
        if (captured) {
            pendingPhotoUri = null
            onPhotosSelected(listOf(photoUri.toString()))
        } else {
            discardPhoto(photoUri)
        }
    }

    return PhotoInput(
        choosePhotos = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        takePhoto = {
            val photoUri = newPhotoUri().toUri()
            pendingPhotoUri = photoUri
            try {
                camera.launch(photoUri)
            } catch (_: ActivityNotFoundException) {
                // No camera app, as on some tablets or where a device policy disables the camera.
                discardPhoto(photoUri)
            }
        },
    )
}
