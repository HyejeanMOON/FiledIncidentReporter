package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.feature.common.IncidentErrorDialog
import com.hyejeanmoon.fieldincidentreporter.feature.common.LoadingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.common.MissingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.messageResId

@Composable
fun IncidentDetailRoute(
    incidentId: String?,
    refreshVersion: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: (String) -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.isDeleting) { }
    val currentOnDeleted by rememberUpdatedState(onDeleted)

    LaunchedEffect(viewModel, incidentId, refreshVersion) {
        if (incidentId != null) viewModel.load(incidentId, refresh = refreshVersion > 0)
    }
    val deletedIncidentId = uiState.deletedIncidentId
    val loadFailure = uiState.loadFailure
    LaunchedEffect(viewModel, deletedIncidentId) {
        if (deletedIncidentId != null) {
            currentOnDeleted(deletedIncidentId)
            viewModel.onAction(IncidentDetailAction.DeleteAcknowledged(deletedIncidentId))
        }
    }

    when {
        incidentId == null -> MissingIncidentScreen(onBack = onBack)
        uiState.incident == null && loadFailure != null -> MissingIncidentScreen(
            onBack = onBack,
            message = stringResource(loadFailure.messageResId()),
            onRetry = if (loadFailure != IncidentFailure.NOT_FOUND) {
                { viewModel.load(incidentId, refresh = true) }
            } else null,
        )
        uiState.incident == null -> LoadingIncidentScreen()
        else -> IncidentDetailScreen(
            uiState = uiState,
            onBack = onBack,
            onEdit = onEdit,
            onDelete = { viewModel.onAction(IncidentDetailAction.Delete) },
        )
    }

    uiState.error?.let { error ->
        IncidentErrorDialog(failure = error, onDismiss = {
            viewModel.onAction(IncidentDetailAction.ErrorDismissed)
        })
    }
}
