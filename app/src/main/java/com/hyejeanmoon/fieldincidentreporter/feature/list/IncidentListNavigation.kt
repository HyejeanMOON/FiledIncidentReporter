package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object IncidentListRoute

private const val RefreshAfterMutationKey = "refreshAfterMutation"

/** Returns to the list after a create, update, or delete, and asks it to refresh from the server. */
fun NavController.returnToIncidentListAfterMutation() {
    val listEntry = getBackStackEntry(IncidentListRoute)
    if (popBackStack(IncidentListRoute, inclusive = false)) {
        listEntry.savedStateHandle[RefreshAfterMutationKey] = true
    }
}

fun NavGraphBuilder.incidentListScreen(
    onReportIncident: () -> Unit,
    onIncidentClick: (String) -> Unit,
) {
    composable<IncidentListRoute> { entry ->
        val refreshRequested by entry.savedStateHandle.getStateFlow(RefreshAfterMutationKey, false)
            .collectAsStateWithLifecycle()
        IncidentListScreen(
            refreshRequested = refreshRequested,
            onRefreshRequestHandled = { entry.savedStateHandle[RefreshAfterMutationKey] = false },
            onReportIncident = onReportIncident,
            onIncidentClick = onIncidentClick,
        )
    }
}
