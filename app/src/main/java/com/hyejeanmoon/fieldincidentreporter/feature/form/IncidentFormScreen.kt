package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFormScreen(
    incident: Incident?,
    photoUris: List<String>,
    onBack: () -> Unit,
    onChoosePhotos: () -> Unit,
    onRemovePhotos: () -> Unit,
    onSave: (title: String, location: String, description: String, photos: List<String>) -> Unit,
    isSaving: Boolean,
) {
    var title by rememberSaveable(incident?.id) { mutableStateOf(incident?.title.orEmpty()) }
    var location by rememberSaveable(incident?.id) { mutableStateOf(incident?.location.orEmpty()) }
    var description by rememberSaveable(incident?.id) { mutableStateOf(incident?.description.orEmpty()) }
    var validationMessage by rememberSaveable(incident?.id) { mutableStateOf<String?>(null) }
    val requiredFieldsMessage = stringResource(R.string.validation_required_fields)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (incident == null) R.string.new_incident_title else R.string.edit_incident_title,
                        ),
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
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
                value = title,
                onValueChange = { title = it; validationMessage = null },
                label = { Text(stringResource(R.string.incident_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it; validationMessage = null },
                label = { Text(stringResource(R.string.location_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; validationMessage = null },
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.photos_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.photos_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onChoosePhotos) {
                    Text(stringResource(R.string.choose_photos))
                }
                if (photoUris.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.photos_selected, photoUris.size),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(onClick = onRemovePhotos) {
                        Text(stringResource(R.string.remove_photos))
                    }
                }
            }
            validationMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = {
                    if (title.isBlank() || location.isBlank() || description.isBlank()) {
                        validationMessage = requiredFieldsMessage
                    } else {
                        onSave(title.trim(), location.trim(), description.trim(), photoUris)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            ) {
                if (isSaving) CircularProgressIndicator()
                else Text(stringResource(if (incident == null) R.string.submit_incident else R.string.save_changes))
            }
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
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
            incident = null,
            photoUris = emptyList(),
            onBack = {},
            onChoosePhotos = {},
            onRemovePhotos = {},
            onSave = { _, _, _, _ -> },
            isSaving = false,
        )
    }
}

@Preview(name = "Edit incident form", showBackground = true)
@Composable
private fun EditIncidentFormScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentFormScreen(
            incident = PreviewIncident,
            photoUris = PreviewIncident.photoUris,
            onBack = {},
            onChoosePhotos = {},
            onRemovePhotos = {},
            onSave = { _, _, _, _ -> },
            isSaving = false,
        )
    }
}
