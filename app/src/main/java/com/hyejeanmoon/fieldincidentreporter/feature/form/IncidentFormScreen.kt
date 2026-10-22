package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.feature.common.LoadingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.common.IncidentPhotoImage
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
            RequiredIncidentTextField(
                value = uiState.title,
                onValueChange = { onEvent(IncidentFormUiEvent.TitleChanged(it)) },
                field = RequiredIncidentField.TITLE,
                labelRes = R.string.incident_title_label,
                isError = RequiredIncidentField.TITLE in visibleMissingFields,
                enabled = canEdit,
                onEvent = onEvent,
            )
            RequiredIncidentTextField(
                value = uiState.location,
                onValueChange = { onEvent(IncidentFormUiEvent.LocationChanged(it)) },
                field = RequiredIncidentField.LOCATION,
                labelRes = R.string.location_label,
                isError = RequiredIncidentField.LOCATION in visibleMissingFields,
                enabled = canEdit,
                onEvent = onEvent,
            )
            RequiredIncidentTextField(
                value = uiState.description,
                onValueChange = { onEvent(IncidentFormUiEvent.DescriptionChanged(it)) },
                field = RequiredIncidentField.DESCRIPTION,
                labelRes = R.string.description_label,
                isError = RequiredIncidentField.DESCRIPTION in visibleMissingFields,
                enabled = canEdit,
                onEvent = onEvent,
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
                if (uiState.photos.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.photos_selected, uiState.photos.size),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.photos.size) { index ->
                            val photo = uiState.photos[index]
                            Column {
                                IncidentPhotoImage(
                                    uri = photo.displayUri,
                                    index = index,
                                    modifier = Modifier.size(96.dp),
                                )
                                TextButton(
                                    onClick = { onEvent(IncidentFormUiEvent.PhotoRemoved(photo)) },
                                    enabled = canEdit,
                                ) {
                                    Text(stringResource(R.string.remove_photo))
                                }
                            }
                        }
                    }
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
    if (uiState.isSaving && uiState.incidentId == null) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            Surface(shape = MaterialTheme.shapes.large) {
                LoadingIncidentScreen(
                    message = stringResource(R.string.submitting_incident),
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RequiredIncidentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    field: RequiredIncidentField,
    @StringRes labelRes: Int,
    isError: Boolean,
    enabled: Boolean,
    onEvent: (IncidentFormUiEvent) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
            onEvent(IncidentFormUiEvent.FieldFocusChanged(field, focusState.isFocused))
        },
        singleLine = field != RequiredIncidentField.DESCRIPTION,
        minLines = if (field == RequiredIncidentField.DESCRIPTION) 4 else 1,
        isError = isError,
        supportingText = { if (isError) Text(stringResource(R.string.required_field_error)) },
        enabled = enabled,
    )
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
