package com.hyejeanmoon.fieldincidentreporter.feature.form

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.feature.common.IncidentErrorDialog
import java.io.File
import java.util.UUID

@Composable
fun IncidentFormRoute(
    incident: Incident?,
    onBack: () -> Unit,
    onSaved: (Incident) -> Unit,
    viewModel: IncidentFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.isSaving) { }
    val currentOnSaved by rememberUpdatedState(onSaved)
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    var pendingPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(viewModel, incident?.id) {
        viewModel.initialize(incident)
    }
    val savedIncident = uiState.savedIncident
    LaunchedEffect(viewModel, savedIncident) {
        if (savedIncident != null) {
            currentOnSaved(savedIncident)
            viewModel.acknowledgeSaved(savedIncident)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            selectedUris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Picker fallbacks may provide a temporary read grant only.
                }
            }
            viewModel.onEvent(IncidentFormUiEvent.PhotosSelected(selectedUris.map { it.toString() }))
        } else {
            viewModel.onEvent(IncidentFormUiEvent.PhotoInputCancelled)
        }
    }
    val camera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { captured ->
        val photoUri = pendingPhotoUri
        if (photoUri != null) {
            if (captured) {
                viewModel.onEvent(IncidentFormUiEvent.PhotoCaptured(photoUri.toString()))
            } else {
                contentResolver.delete(photoUri, null, null)
                viewModel.onEvent(IncidentFormUiEvent.PhotoInputCancelled)
            }
            pendingPhotoUri = null
        }
    }

    if (uiState.isInitialized) {
        IncidentFormScreen(
            uiState = uiState,
            onBack = onBack,
            onChoosePhotos = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onTakePhoto = {
                val photoUri = createIncidentPhotoUri(context)
                pendingPhotoUri = photoUri
                camera.launch(photoUri)
            },
            onEvent = viewModel::onEvent,
        )
    }

    uiState.error?.let { error ->
        IncidentErrorDialog(failure = error, onDismiss = viewModel::clearError)
    }
}

private fun createIncidentPhotoUri(context: Context): Uri {
    val photoDirectory = File(context.filesDir, "incident_photos").apply { mkdirs() }
    val photoFile = File(photoDirectory, "incident_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile,
    )
}
