package com.hyejeanmoon.fieldincidentreporter.feature

import android.net.Uri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hyejeanmoon.fieldincidentreporter.feature.common.IncidentErrorDialog
import com.hyejeanmoon.fieldincidentreporter.feature.common.LoadingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.common.MissingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.feature.detail.IncidentDetailRoute
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
        IncidentErrorDialog(failure = error, onDismiss = listViewModel::clearError)
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
                IncidentDetailRoute(
                    incident = incident,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(IncidentDestinations.edit(incident.id)) },
                    onDeleted = { deletedId ->
                        listViewModel.onIncidentDeleted(entry.id, deletedId)
                    },
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
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(ScreenTransitionDurationMillis))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(ScreenTransitionDurationMillis))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(ScreenTransitionDurationMillis))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ScreenTransitionDurationMillis, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(ScreenTransitionDurationMillis))
            },
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
