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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
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
private const val DetailRefreshVersionKey = "detail_refresh_version"

@Composable
fun IncidentApp() {
    val listViewModel: IncidentListViewModel = hiltViewModel()
    val incidents = listViewModel.incidents.collectAsLazyPagingItems()
    val navController = rememberNavController()

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
                    navController.navigate(IncidentDestinations.detail(incident.id))
                },
            )
        }
        composable(
            route = IncidentDestinations.Detail,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("incidentId")
            val refreshVersion by entry.savedStateHandle
                .getStateFlow(DetailRefreshVersionKey, 0)
                .collectAsStateWithLifecycle()
            IncidentDetailRoute(
                incidentId = id,
                refreshVersion = refreshVersion,
                onBack = { navController.popBackStack() },
                onEdit = { if (id != null) navController.navigate(IncidentDestinations.edit(id)) },
                onDeleted = {
                    incidents.refresh()
                    if (navController.currentBackStackEntry?.id == entry.id) navController.popBackStack()
                },
            )
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
            IncidentFormRoute(
                incidentId = id,
                onBack = { navController.popBackStack() },
                onSaved = { savedIncident ->
                    if (id != null && savedIncident.id == id) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            handle[DetailRefreshVersionKey] = (handle.get<Int>(DetailRefreshVersionKey) ?: 0) + 1
                        }
                    }
                    incidents.refresh()
                    if (navController.currentBackStackEntry?.id == entry.id) navController.popBackStack()
                },
            )
        }
    }
}
