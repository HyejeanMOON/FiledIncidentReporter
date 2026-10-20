package com.hyejeanmoon.fieldincidentreporter.feature.incidents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    incidents: List<Incident>,
    onReportIncident: () -> Unit,
    onIncidentClick: (Incident) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportIncident,
            ) {
                Text(stringResource(R.string.report_incident))
            }
        },
    ) { contentPadding ->
        if (incidents.isEmpty()) {
            EmptyIncidentList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                onReportIncident = onReportIncident,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 16.dp + contentPadding.calculateTopPadding(),
                    end = 20.dp,
                    bottom = 96.dp + contentPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.incident_list_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(incidents, key = { it.id }) { incident ->
                    IncidentCard(incident = incident, onClick = { onIncidentClick(incident) })
                }
            }
        }
    }
}

@Composable
private fun EmptyIncidentList(modifier: Modifier = Modifier, onReportIncident: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_list_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.empty_list_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onReportIncident) {
            Text(stringResource(R.string.report_incident))
        }
    }
}

@Composable
private fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = incident.location,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (incident.photoUris.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.photo_count, incident.photoUris.size),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFormScreen(
    incident: Incident?,
    photoUris: List<String>,
    onBack: () -> Unit,
    onChoosePhotos: () -> Unit,
    onRemovePhotos: () -> Unit,
    onSave: (title: String, location: String, description: String, photos: List<String>) -> Unit,
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
            ) {
                Text(
                    stringResource(
                        if (incident == null) R.string.submit_incident else R.string.save_changes,
                    ),
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incident: Incident,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirmation by rememberSaveable(incident.id) { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.incident_details_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
                actions = {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = incident.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            DetailField(label = stringResource(R.string.location_label), value = incident.location)
            DetailField(label = stringResource(R.string.description_label), value = incident.description)
            DetailField(
                label = stringResource(R.string.photos_label),
                value = stringResource(R.string.photo_count, incident.photoUris.size),
            )
            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.delete_incident), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingIncidentScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.incident_details_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.incident_missing))
            TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_list)) }
        }
    }
}


private val PreviewIncident = Incident(
    id = "preview-hydraulic-press",
    title = "Hydraulic press is not responding",
    location = "Workshop · Bay 3",
    description = "The control panel does not respond when the machine is switched on.",
)

@Preview(name = "Incident list", showBackground = true)
@Composable
private fun IncidentListScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentListScreen(
            incidents = listOf(PreviewIncident),
            onReportIncident = {},
            onIncidentClick = {},
        )
    }
}

@Preview(name = "Empty incident list", showBackground = true)
@Composable
private fun EmptyIncidentListScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentListScreen(
            incidents = emptyList(),
            onReportIncident = {},
            onIncidentClick = {},
        )
    }
}

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
        )
    }
}

@Preview(name = "Incident details", showBackground = true)
@Composable
private fun IncidentDetailScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentDetailScreen(
            incident = PreviewIncident,
            onBack = {},
            onEdit = {},
            onDelete = {},
        )
    }
}
