package com.hyejeanmoon.fieldincidentreporter.feature.form

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
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val photoUri = pendingPhotoUri ?: return@rememberLauncherForActivityResult
        pendingPhotoUri = null
        if (captured) {
            onPhotosSelected(listOf(photoUri.toString()))
        } else {
            context.contentResolver.delete(photoUri, null, null)
            onCancelled()
        }
    }

    return PhotoInput(
        choosePhotos = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        takePhoto = {
            val photoUri = Uri.parse(newPhotoUri())
            pendingPhotoUri = photoUri
            camera.launch(photoUri)
        },
    )
}
