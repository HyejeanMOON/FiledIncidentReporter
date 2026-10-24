package com.hyejeanmoon.fieldincidentreporter.feature.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Creates a report when [incidentId] is null, otherwise edits it. */
@Serializable
data class IncidentFormRoute(val incidentId: String? = null)

internal fun SavedStateHandle.toIncidentFormRoute() = IncidentFormRoute(get<String>(IncidentFormRoute::incidentId.name))

fun NavController.navigateToIncidentForm(incidentId: String? = null) {
    navigate(IncidentFormRoute(incidentId)) { launchSingleTop = true }
}

fun NavGraphBuilder.incidentFormScreen(onBack: () -> Unit, onCreated: () -> Unit, onUpdated: () -> Unit) {
    composable<IncidentFormRoute> {
        IncidentFormScreen(onBack = dropUnlessResumed(block = onBack), onCreated = onCreated, onUpdated = onUpdated)
    }
}
