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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
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
    val incidents = listViewModel.incidents.collectAsLazyPagingItems()
    val navController = rememberNavController()

    val deletedFromEntryId = listState.deletedFromEntryId
    LaunchedEffect(deletedFromEntryId, navController) {
        if (deletedFromEntryId != null) {
            if (navController.currentBackStackEntry?.id == deletedFromEntryId) {
                navController.popBackStack()
            }
            listViewModel.acknowledgeDeletion(deletedFromEntryId)
        }
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
                incidents = incidents,
                onReportIncident = { navController.navigate("report") },
                onIncidentClick = { incident ->
                    listViewModel.onIncidentSelected(incident)
                    navController.navigate(IncidentDestinations.detail(incident.id))
                },
            )
        }
        composable(
            route = IncidentDestinations.Detail,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("incidentId")
            val incident = listState.selectedIncident?.takeIf { it.id == id }
                ?: incidents.itemSnapshotList.items.firstOrNull { it.id == id }
            LaunchedEffect(id, incident?.id) {
                if (id != null && incident == null) listViewModel.ensureIncident(id)
            }
            val lookupFailure = listState.lookupFailure.takeIf { listState.failedIncidentId == id }
            if (id != null && incident == null && lookupFailure == null) {
                LoadingIncidentScreen()
            } else if (incident == null) {
                MissingIncidentScreen(
                    onBack = { navController.popBackStack() },
                    message = lookupFailure?.let { stringResource(it.messageResId()) },
                    onRetry = if (lookupFailure != null && lookupFailure != IncidentFailure.NOT_FOUND && id != null) {
                        { listViewModel.ensureIncident(id) }
                    } else null,
                )
            } else {
                IncidentDetailRoute(
                    incident = incident,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(IncidentDestinations.edit(incident.id)) },
                    onDeleted = { deletedId ->
                        listViewModel.onIncidentDeleted(entry.id, deletedId)
                        incidents.refresh()
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
            val incident = listState.selectedIncident?.takeIf { it.id == id }
                ?: incidents.itemSnapshotList.items.firstOrNull { it.id == id }
            LaunchedEffect(id, incident?.id) {
                if (id != null && incident == null) listViewModel.ensureIncident(id)
            }
            val lookupFailure = listState.lookupFailure.takeIf { listState.failedIncidentId == id }
            if (id != null && incident == null && lookupFailure == null) {
                LoadingIncidentScreen()
            } else if (id != null && incident == null) {
                MissingIncidentScreen(
                    onBack = { navController.popBackStack() },
                    message = lookupFailure?.let { stringResource(it.messageResId()) },
                    onRetry = if (lookupFailure != null && lookupFailure != IncidentFailure.NOT_FOUND) {
                        { listViewModel.ensureIncident(id) }
                    } else null,
                )
            } else {
                IncidentFormRoute(
                    incident = incident,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedIncident ->
                        listViewModel.onIncidentSaved(savedIncident)
                        incidents.refresh()
                        if (navController.currentBackStackEntry?.id == entry.id) {
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}
