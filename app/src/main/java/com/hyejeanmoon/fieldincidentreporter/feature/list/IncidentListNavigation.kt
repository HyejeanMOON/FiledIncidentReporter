package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object IncidentListRoute

fun NavGraphBuilder.incidentListScreen(
    onReportIncident: () -> Unit,
    onIncidentClick: (String) -> Unit,
) {
    composable<IncidentListRoute> {
        IncidentListScreen(onReportIncident = onReportIncident, onIncidentClick = onIncidentClick)
    }
}
