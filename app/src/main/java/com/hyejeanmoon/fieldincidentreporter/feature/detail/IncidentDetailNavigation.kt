package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.lifecycle.SavedStateHandle
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

fun NavGraphBuilder.incidentDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    composable<IncidentDetailRoute> {
        IncidentDetailScreen(
            onBack = dropUnlessResumed(block = onBack),
            onDeleted = onBack,
            onEdit = onEdit,
        )
    }
}
