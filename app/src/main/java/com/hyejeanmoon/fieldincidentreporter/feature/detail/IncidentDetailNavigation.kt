package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class IncidentDetailRoute(val incidentId: String)

/** Navigation stores route arguments in the destination's [SavedStateHandle] under their property names. */
internal fun SavedStateHandle.toIncidentDetailRoute() =
    IncidentDetailRoute(checkNotNull(get<String>(IncidentDetailRoute::incidentId.name)))

fun NavController.navigateToIncidentDetail(incidentId: String) {
    navigate(IncidentDetailRoute(incidentId)) { launchSingleTop = true }
}

private const val ReloadAfterEditKey = "reloadAfterEdit"

/** Returns to the edited report's details and asks them to reload from the server. */
fun NavController.returnToIncidentDetailAfterEdit() {
    val detailEntry = getBackStackEntry<IncidentDetailRoute>()
    if (popBackStack<IncidentDetailRoute>(inclusive = false)) {
        detailEntry.savedStateHandle[ReloadAfterEditKey] = true
    }
}

fun NavGraphBuilder.incidentDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEdit: (String) -> Unit,
) {
    composable<IncidentDetailRoute> { entry ->
        val reloadRequested by entry.savedStateHandle.getStateFlow(ReloadAfterEditKey, false)
            .collectAsStateWithLifecycle()
        IncidentDetailScreen(
            reloadRequested = reloadRequested,
            onReloadRequestHandled = { entry.savedStateHandle[ReloadAfterEditKey] = false },
            onBack = dropUnlessResumed(block = onBack),
            onDeleted = onDeleted,
            onEdit = onEdit,
        )
    }
}
