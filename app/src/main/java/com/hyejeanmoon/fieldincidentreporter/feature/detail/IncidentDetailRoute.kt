package com.hyejeanmoon.fieldincidentreporter.feature.detail

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
fun IncidentDetailRoute(
    incident: Incident,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: (String) -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDeleted by rememberUpdatedState(onDeleted)

    LaunchedEffect(viewModel, incident) {
        viewModel.onAction(IncidentDetailAction.IncidentChanged(incident))
    }
    val deletedIncidentId = uiState.deletedIncidentId
    LaunchedEffect(viewModel, deletedIncidentId) {
        if (deletedIncidentId != null) {
            currentOnDeleted(deletedIncidentId)
            viewModel.onAction(IncidentDetailAction.DeleteAcknowledged(deletedIncidentId))
        }
    }

    IncidentDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = { viewModel.onAction(IncidentDetailAction.Delete) },
    )

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(IncidentDetailAction.ErrorDismissed) },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(error.messageResId())) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(IncidentDetailAction.ErrorDismissed) }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}
