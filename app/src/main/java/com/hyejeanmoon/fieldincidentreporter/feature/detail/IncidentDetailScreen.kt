package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.ui.component.BackTopAppBar
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentErrorDialog
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentLoadFailureScreen
import com.hyejeanmoon.fieldincidentreporter.ui.component.LabeledText
import com.hyejeanmoon.fieldincidentreporter.ui.component.LoadingContent
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme
import com.hyejeanmoon.fieldincidentreporter.ui.util.formatIncidentTime
import java.time.Instant

@Composable
internal fun IncidentDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.isDeleting) { }
    if (uiState.isDeleted) LaunchedEffect(Unit) { onDeleted() }

    val incident = uiState.incident
    val loadFailure = uiState.loadFailure
    when {
        incident != null -> IncidentDetailContent(
            incident = incident,
            isDeleting = uiState.isDeleting,
            onBack = onBack,
            onEdit = { onEdit(incident.id) },
            onDelete = { viewModel.onAction(IncidentDetailAction.Delete) },
        )
        loadFailure != null -> IncidentLoadFailureScreen(
            title = stringResource(R.string.incident_details_title),
            failure = loadFailure,
            onBack = onBack,
            onRetry = { viewModel.onAction(IncidentDetailAction.Retry) },
        )
        else -> LoadingContent(Modifier.fillMaxSize())
    }

    uiState.error?.let { error ->
        IncidentErrorDialog(failure = error, onDismiss = { viewModel.onAction(IncidentDetailAction.ErrorDismissed) })
    }
}

@Composable
internal fun IncidentDetailContent(
    incident: Incident,
    isDeleting: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirmation by rememberSaveable(incident.id) { mutableStateOf(false) }
    Scaffold(
        topBar = {
            BackTopAppBar(
                title = stringResource(R.string.incident_details_title),
                onBack = onBack,
                backEnabled = !isDeleting,
            ) {
                TextButton(onClick = { showDeleteConfirmation = true }, enabled = !isDeleting) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onEdit, enabled = !isDeleting) {
                    Text(stringResource(R.string.edit))
                }
            }
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
            LabeledText(stringResource(R.string.location_label), incident.location)
            LabeledText(stringResource(R.string.description_label), incident.description)
            LabeledText(stringResource(R.string.created_at_label), formatIncidentTime(incident.createdAt))
            if (incident.isEdited) {
                LabeledText(stringResource(R.string.updated_at_label), formatIncidentTime(incident.updatedAt))
            }
            LabeledText(
                stringResource(R.string.photos_label),
                stringResource(R.string.photo_count, incident.photos.size),
            )
            IncidentPhotoGallery(photoUrls = incident.photos.map { it.displayUri })
        }
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = { showDeleteConfirmation = false; onDelete() },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirmation_title)) },
        text = { Text(stringResource(R.string.delete_confirmation_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(name = "Incident details", showBackground = true)
@Composable
private fun IncidentDetailContentPreview() {
    FieldIncidentReporterTheme {
        IncidentDetailContent(
            incident = Incident(
                id = "preview-hydraulic-press",
                title = "Hydraulic press is not responding",
                location = "Workshop · Bay 3",
                description = "The control panel does not respond when the machine is switched on.",
                createdAt = Instant.parse("2026-10-20T09:00:00Z"),
                updatedAt = Instant.parse("2026-10-21T11:30:00Z"),
            ),
            isDeleting = false,
            onBack = {},
            onEdit = {},
            onDelete = {},
        )
    }
}
