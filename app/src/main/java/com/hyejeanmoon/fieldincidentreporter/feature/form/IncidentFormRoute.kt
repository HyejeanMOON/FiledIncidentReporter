package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.feature.messageResId

@Composable
fun IncidentFormRoute(
    incident: Incident?,
    onBack: () -> Unit,
    onSaved: (Incident) -> Unit,
    viewModel: IncidentFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)

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
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            viewModel.onEvent(IncidentFormUiEvent.PhotosSelected(selectedUris.map { it.toString() }))
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
            onEvent = viewModel::onEvent,
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(error.messageResId())) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}
