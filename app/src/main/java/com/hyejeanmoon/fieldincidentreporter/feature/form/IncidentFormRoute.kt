package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident

@Composable
fun IncidentFormRoute(
    incident: Incident?,
    onBack: () -> Unit,
    onSave: (title: String, location: String, description: String, photos: List<String>) -> Unit,
    isSaving: Boolean,
) {
    var photoUris by rememberSaveable(incident?.id) {
        mutableStateOf(incident?.photoUris.orEmpty())
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            photoUris = selectedUris.map { it.toString() }
        }
    }

    IncidentFormScreen(
        incident = incident,
        photoUris = photoUris,
        onBack = onBack,
        onChoosePhotos = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRemovePhotos = { photoUris = emptyList() },
        onSave = onSave,
        isSaving = isSaving,
    )
}
