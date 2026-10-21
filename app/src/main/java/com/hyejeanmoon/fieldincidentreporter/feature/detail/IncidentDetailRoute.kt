package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.feature.common.IncidentErrorDialog

@Composable
fun IncidentDetailRoute(
    incident: Incident,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: (String) -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.isDeleting) { }
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
        IncidentErrorDialog(failure = error, onDismiss = {
            viewModel.onAction(IncidentDetailAction.ErrorDismissed)
        })
    }
}
