package com.hyejeanmoon.fieldincidentreporter.feature

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.repository.IncidentRepository
import com.hyejeanmoon.fieldincidentreporter.feature.detail.IncidentDetailScreen
import com.hyejeanmoon.fieldincidentreporter.feature.detail.LoadingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.detail.MissingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.form.IncidentFormRoute
import com.hyejeanmoon.fieldincidentreporter.feature.form.IncidentFormViewModel
import com.hyejeanmoon.fieldincidentreporter.feature.list.IncidentListScreen
import com.hyejeanmoon.fieldincidentreporter.feature.list.IncidentListViewModel

private object IncidentDestinations {
    const val List = "incidents"
    const val Detail = "incident/{incidentId}"
    const val Report = "report?incidentId={incidentId}"

    fun detail(id: String) = "incident/$id"
    fun edit(id: String) = "report?incidentId=$id"
}

@Composable
fun IncidentApp(repository: IncidentRepository) {
    val listViewModel: IncidentListViewModel = viewModel(factory = IncidentListViewModel.factory(repository))
    val formViewModel: IncidentFormViewModel = viewModel(factory = IncidentFormViewModel.factory(repository))
    val listState by listViewModel.uiState.collectAsStateWithLifecycle()
    val formState by formViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(listViewModel, navController) {
        listViewModel.events.collect { event ->
            if (navController.currentBackStackEntry?.id == event.deletedFromEntryId) {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(formViewModel, listViewModel, navController) {
        formViewModel.events.collect { event ->
            listViewModel.onIncidentSaved(event.savedIncident)
            if (navController.currentBackStackEntry?.id == event.originEntryId) {
                navController.popBackStack()
            }
        }
    }

    (formState.error ?: listState.error)?.let { error ->
        val message = when (error) {
            IncidentFailure.NETWORK -> R.string.error_network
            IncidentFailure.SERVER -> R.string.error_server
            IncidentFailure.NOT_FOUND -> R.string.error_not_found
            IncidentFailure.UNKNOWN -> R.string.error_unknown
        }
        val clearError = if (formState.error != null) formViewModel::clearError else listViewModel::clearError
        AlertDialog(
            onDismissRequest = clearError,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = clearError) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    NavHost(navController = navController, startDestination = IncidentDestinations.List) {
        composable(IncidentDestinations.List) {
            IncidentListScreen(
                incidents = listState.incidents,
                isLoading = listState.isLoading,
                onRefresh = listViewModel::refresh,
                onReportIncident = { navController.navigate("report") },
                onIncidentClick = { incident ->
                    navController.navigate(IncidentDestinations.detail(incident.id))
                },
            )
        }
        composable(
            route = IncidentDestinations.Detail,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("incidentId")
            val incident = listState.incidents.firstOrNull { it.id == id }
            if (incident == null && listState.isLoading) {
                LoadingIncidentScreen()
            } else if (incident == null) {
                MissingIncidentScreen(onBack = { navController.popBackStack() })
            } else {
                IncidentDetailScreen(
                    incident = incident,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(IncidentDestinations.edit(incident.id)) },
                    onDelete = {
                        listViewModel.removeIncident(entry.id, incident.id)
                    },
                    isDeleting = listState.isDeleting,
                )
            }
        }
        composable(
            route = IncidentDestinations.Report,
            arguments = listOf(
                navArgument("incidentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("incidentId")
            val incident = listState.incidents.firstOrNull { it.id == id }
            if (id != null && incident == null && listState.isLoading) {
                LoadingIncidentScreen()
            } else if (id != null && incident == null) {
                MissingIncidentScreen(onBack = { navController.popBackStack() })
            } else {
                IncidentFormRoute(
                    incident = incident,
                    onBack = { navController.popBackStack() },
                    onSave = { title, location, description, photos ->
                        formViewModel.saveIncident(entry.id, id, title, location, description, photos)
                    },
                    isSaving = formState.isSaving,
                )
            }
        }
    }
}
