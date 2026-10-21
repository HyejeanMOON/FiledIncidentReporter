package com.hyejeanmoon.fieldincidentreporter.feature

import android.net.Uri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.feature.detail.IncidentDetailScreen
import com.hyejeanmoon.fieldincidentreporter.feature.detail.LoadingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.detail.MissingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.form.IncidentFormRoute
import com.hyejeanmoon.fieldincidentreporter.feature.list.IncidentListScreen
import com.hyejeanmoon.fieldincidentreporter.feature.list.IncidentListViewModel

private object IncidentDestinations {
    const val List = "incidents"
    const val Detail = "incident/{incidentId}"
    const val Report = "report?incidentId={incidentId}"

    fun detail(id: String) = "incident/${Uri.encode(id)}"
    fun edit(id: String) = "report?incidentId=${Uri.encode(id)}"
}

private const val ScreenTransitionDurationMillis = 320

@Composable
fun IncidentApp() {
    val listViewModel: IncidentListViewModel = hiltViewModel()
    val listState by listViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()

    val deletedFromEntryId = listState.deletedFromEntryId
    LaunchedEffect(deletedFromEntryId, navController) {
        if (deletedFromEntryId != null) {
            if (navController.currentBackStackEntry?.id == deletedFromEntryId) {
                navController.popBackStack()
            }
            listViewModel.acknowledgeDeletion(deletedFromEntryId)
        }
    }

    val listError = if (currentEntry?.destination?.route == IncidentDestinations.Report) null else listState.error
    listError?.let { error ->
        AlertDialog(
            onDismissRequest = listViewModel::clearError,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(error.messageResId())) },
            confirmButton = {
                TextButton(onClick = listViewModel::clearError) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    NavHost(
        navController = navController,
        startDestination = IncidentDestinations.List,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
            )
        },
    ) {
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
                    onSaved = { savedIncident ->
                        listViewModel.onIncidentSaved(savedIncident)
                        if (navController.currentBackStackEntry?.id == entry.id) {
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}
