package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFormScreen(
    uiState: IncidentFormUiState,
    onBack: () -> Unit,
    onChoosePhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onEvent: (IncidentFormUiEvent) -> Unit,
) {
    val visibleMissingFields = uiState.missingFields.filter { it in uiState.validatedFields }
    val titleError = RequiredIncidentField.TITLE in visibleMissingFields
    val locationError = RequiredIncidentField.LOCATION in visibleMissingFields
    val descriptionError = RequiredIncidentField.DESCRIPTION in visibleMissingFields
    val photosError = RequiredIncidentField.PHOTOS in visibleMissingFields
    val canEdit = !uiState.isSaving && uiState.savedIncident == null
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.incidentId == null) R.string.new_incident_title else R.string.edit_incident_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isSaving) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.form_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { onEvent(IncidentFormUiEvent.TitleChanged(it)) },
                label = { Text(stringResource(R.string.incident_title_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        onEvent(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.TITLE, focusState.isFocused))
                    },
                singleLine = true,
                isError = titleError,
                supportingText = {
                    if (titleError) Text(stringResource(R.string.required_field_error))
                },
                enabled = canEdit,
            )
            OutlinedTextField(
                value = uiState.location,
                onValueChange = { onEvent(IncidentFormUiEvent.LocationChanged(it)) },
                label = { Text(stringResource(R.string.location_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        onEvent(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.LOCATION, focusState.isFocused))
                    },
                singleLine = true,
                isError = locationError,
                supportingText = {
                    if (locationError) Text(stringResource(R.string.required_field_error))
                },
                enabled = canEdit,
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onEvent(IncidentFormUiEvent.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        onEvent(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.DESCRIPTION, focusState.isFocused))
                    },
                minLines = 4,
                isError = descriptionError,
                supportingText = {
                    if (descriptionError) Text(stringResource(R.string.required_field_error))
                },
                enabled = canEdit,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (photosError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.photos_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (photosError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = stringResource(R.string.photos_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (photosError) {
                    Text(
                        text = stringResource(R.string.required_photos_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onChoosePhotos,
                        modifier = Modifier.onFocusChanged { focusState ->
                            onEvent(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.PHOTOS, focusState.isFocused))
                        },
                        enabled = canEdit,
                    ) {
                        Text(stringResource(R.string.choose_photos))
                    }
                    OutlinedButton(
                        onClick = onTakePhoto,
                        modifier = Modifier.onFocusChanged { focusState ->
                            onEvent(IncidentFormUiEvent.FieldFocusChanged(RequiredIncidentField.PHOTOS, focusState.isFocused))
                        },
                        enabled = canEdit,
                    ) {
                        Text(stringResource(R.string.take_photo))
                    }
                }
                if (uiState.photoUris.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.photos_selected, uiState.photoUris.size),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(
                        onClick = { onEvent(IncidentFormUiEvent.PhotosRemoved) },
                        enabled = canEdit,
                    ) {
                        Text(stringResource(R.string.remove_photos))
                    }
                }
            }
            Button(
                onClick = { onEvent(IncidentFormUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isSubmitEnabled,
            ) {
                if (uiState.isSaving) CircularProgressIndicator()
                else Text(stringResource(if (uiState.incidentId == null) R.string.submit_incident else R.string.save_changes))
            }
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

private val PreviewIncident = Incident(
    id = "preview-hydraulic-press",
    title = "Hydraulic press is not responding",
    location = "Workshop · Bay 3",
    description = "The control panel does not respond when the machine is switched on.",
)

@Preview(name = "New incident form", showBackground = true)
@Composable
private fun NewIncidentFormScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentFormScreen(
            uiState = IncidentFormUiStateReducer.reduce(
                IncidentFormUiState(),
                IncidentFormStateChange.Initialized(null),
            ),
            onBack = {},
            onChoosePhotos = {},
            onTakePhoto = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Edit incident form", showBackground = true)
@Composable
private fun EditIncidentFormScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentFormScreen(
            uiState = IncidentFormUiStateReducer.reduce(
                IncidentFormUiState(),
                IncidentFormStateChange.Initialized(PreviewIncident),
            ),
            onBack = {},
            onChoosePhotos = {},
            onTakePhoto = {},
            onEvent = {},
        )
    }
}
