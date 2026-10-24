package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_DESCRIPTION_LENGTH
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_LOCATION_LENGTH
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_TITLE_LENGTH
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import com.hyejeanmoon.fieldincidentreporter.ui.component.BackTopAppBar
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentErrorDialog
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentLoadFailureScreen
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentPhotoThumbnail
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentPhotoViewer
import com.hyejeanmoon.fieldincidentreporter.ui.component.LoadingContent
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme

@Composable
internal fun IncidentFormScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onUpdated: () -> Unit,
    viewModel: IncidentFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoInput = rememberPhotoInput(
        newPhotoUri = viewModel::newCapturedPhotoUri,
        onPhotosSelected = { viewModel.onEvent(IncidentFormUiEvent.PhotosSelected(it)) },
        onCancelled = { viewModel.onEvent(IncidentFormUiEvent.PhotoInputCancelled) },
    )

    when (val state = uiState) {
        IncidentFormUiState.Loading -> LoadingContent(Modifier.fillMaxSize())
        is IncidentFormUiState.LoadFailed -> IncidentLoadFailureScreen(
            title = stringResource(R.string.edit_incident_title),
            failure = state.failure,
            onBack = onBack,
            onRetry = { viewModel.onEvent(IncidentFormUiEvent.Retry) },
        )
        is IncidentFormUiState.Ready -> {
            BackHandler(enabled = state.isSaving) { }
            if (state.isSaved) LaunchedEffect(Unit) { if (state.isEditing) onUpdated() else onCreated() }
            IncidentFormContent(
                uiState = state,
                onBack = onBack,
                onChoosePhotos = photoInput.choosePhotos,
                onTakePhoto = photoInput.takePhoto,
                onEvent = viewModel::onEvent,
            )
            state.error?.let { error ->
                IncidentErrorDialog(failure = error, onDismiss = { viewModel.onEvent(IncidentFormUiEvent.ErrorDismissed) })
            }
        }
    }
}

@Composable
internal fun IncidentFormContent(
    uiState: IncidentFormUiState.Ready,
    onBack: () -> Unit,
    onChoosePhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onEvent: (IncidentFormUiEvent) -> Unit,
) {
    val missingFields = uiState.visibleMissingFields
    Scaffold(
        topBar = {
            BackTopAppBar(
                title = stringResource(if (uiState.isEditing) R.string.edit_incident_title else R.string.new_incident_title),
                onBack = onBack,
                backEnabled = !uiState.isSaving,
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
            RequiredTextField(
                field = RequiredIncidentField.TITLE,
                value = uiState.title,
                maxLength = MAX_INCIDENT_TITLE_LENGTH,
                onValueChange = { onEvent(IncidentFormUiEvent.TitleChanged(it)) },
                labelRes = R.string.incident_title_label,
                isError = RequiredIncidentField.TITLE in missingFields,
                enabled = uiState.canEdit,
                onEvent = onEvent,
            )
            RequiredTextField(
                field = RequiredIncidentField.LOCATION,
                value = uiState.location,
                maxLength = MAX_INCIDENT_LOCATION_LENGTH,
                onValueChange = { onEvent(IncidentFormUiEvent.LocationChanged(it)) },
                labelRes = R.string.location_label,
                isError = RequiredIncidentField.LOCATION in missingFields,
                enabled = uiState.canEdit,
                onEvent = onEvent,
            )
            RequiredTextField(
                field = RequiredIncidentField.DESCRIPTION,
                value = uiState.description,
                maxLength = MAX_INCIDENT_DESCRIPTION_LENGTH,
                onValueChange = { onEvent(IncidentFormUiEvent.DescriptionChanged(it)) },
                labelRes = R.string.description_label,
                isError = RequiredIncidentField.DESCRIPTION in missingFields,
                enabled = uiState.canEdit,
                onEvent = onEvent,
            )
            PhotoSection(
                uiState = uiState,
                isError = RequiredIncidentField.PHOTOS in missingFields,
                onChoosePhotos = onChoosePhotos,
                onTakePhoto = onTakePhoto,
                onEvent = onEvent,
            )
            Button(
                onClick = { onEvent(IncidentFormUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isSubmitEnabled,
            ) {
                if (uiState.isSaving) {
                    // Icon-sized, so the button keeps its height while saving.
                    CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(if (uiState.isEditing) R.string.save_changes else R.string.submit_incident))
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isSaving) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
    if (uiState.isSaving && !uiState.isEditing) SubmittingDialog()
}

@Composable
private fun RequiredTextField(
    field: RequiredIncidentField,
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    isError: Boolean,
    enabled: Boolean,
    onEvent: (IncidentFormUiEvent) -> Unit,
) {
    val multiline = field == RequiredIncidentField.DESCRIPTION
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        modifier = Modifier.fillMaxWidth().reportFocus(field, onEvent),
        singleLine = !multiline,
        minLines = if (multiline) 4 else 1,
        isError = isError,
        supportingText = {
            Row {
                if (isError) {
                    Text(
                        text = if (value.isBlank()) {
                            stringResource(R.string.required_field_error)
                        } else {
                            stringResource(R.string.field_too_long_error, maxLength)
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Text(stringResource(R.string.character_count, value.length, maxLength))
            }
        },
        enabled = enabled,
    )
}

@Composable
private fun PhotoSection(
    uiState: IncidentFormUiState.Ready,
    isError: Boolean,
    onChoosePhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onEvent: (IncidentFormUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isError) MaterialTheme.colorScheme.error else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.photos_label),
            style = MaterialTheme.typography.titleSmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.photos_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isError) {
            Text(
                text = stringResource(R.string.required_photos_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        AddPhotosButton(
            enabled = uiState.canEdit,
            onChoosePhotos = onChoosePhotos,
            onTakePhoto = onTakePhoto,
            modifier = Modifier.reportFocus(RequiredIncidentField.PHOTOS, onEvent),
        )
        if (uiState.photos.isNotEmpty()) {
            val photoUris = uiState.photos.map { it.displayUri }
            var viewerPage by rememberSaveable(photoUris) { mutableIntStateOf(-1) }
            Text(
                text = stringResource(R.string.photos_selected, uiState.photos.size),
                style = MaterialTheme.typography.labelMedium,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(uiState.photos) { index, photo ->
                    Column {
                        IncidentPhotoThumbnail(
                            uri = photo.thumbnailUri,
                            index = index,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = stringResource(R.string.view_full_photo),
                                ) { viewerPage = index },
                        )
                        TextButton(
                            onClick = { onEvent(IncidentFormUiEvent.PhotoRemoved(photo)) },
                            enabled = uiState.canEdit,
                        ) {
                            Text(stringResource(R.string.remove_photo))
                        }
                    }
                }
            }
            TextButton(onClick = { onEvent(IncidentFormUiEvent.PhotosRemoved) }, enabled = uiState.canEdit) {
                Text(stringResource(R.string.remove_photos))
            }
            if (viewerPage >= 0) {
                IncidentPhotoViewer(
                    photoUris = photoUris,
                    initialPage = viewerPage,
                    onDismiss = { viewerPage = -1 },
                )
            }
        }
    }
}

@Composable
private fun AddPhotosButton(
    enabled: Boolean,
    onChoosePhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = modifier,
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.add_photos))
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.take_photo)) },
                onClick = {
                    expanded = false
                    onTakePhoto()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.choose_photos)) },
                onClick = {
                    expanded = false
                    onChoosePhotos()
                },
            )
        }
    }
}

@Composable
private fun SubmittingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = MaterialTheme.shapes.large) {
            LoadingContent(Modifier.padding(24.dp), message = stringResource(R.string.submitting_incident))
        }
    }
}

private fun Modifier.reportFocus(field: RequiredIncidentField, onEvent: (IncidentFormUiEvent) -> Unit) =
    onFocusChanged { onEvent(IncidentFormUiEvent.FieldFocusChanged(field, it.isFocused)) }

@Preview(name = "New incident form", showBackground = true)
@Composable
private fun NewIncidentFormPreview() {
    FieldIncidentReporterTheme {
        IncidentFormContent(
            uiState = IncidentFormUiState.Ready(),
            onBack = {},
            onChoosePhotos = {},
            onTakePhoto = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Edit incident form", showBackground = true)
@Composable
private fun EditIncidentFormPreview() {
    FieldIncidentReporterTheme {
        IncidentFormContent(
            uiState = IncidentFormUiState.Ready(
                incidentId = "preview-hydraulic-press",
                title = "Hydraulic press is not responding",
                location = "Workshop · Bay 3",
                description = "The control panel does not respond when the machine is switched on.",
            ),
            onBack = {},
            onChoosePhotos = {},
            onTakePhoto = {},
            onEvent = {},
        )
    }
}
