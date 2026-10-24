package com.hyejeanmoon.fieldincidentreporter.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.hyejeanmoon.fieldincidentreporter.feature.detail.incidentDetailScreen
import com.hyejeanmoon.fieldincidentreporter.feature.detail.navigateToIncidentDetail
import com.hyejeanmoon.fieldincidentreporter.feature.detail.returnToIncidentDetailAfterEdit
import com.hyejeanmoon.fieldincidentreporter.feature.form.incidentFormScreen
import com.hyejeanmoon.fieldincidentreporter.feature.form.navigateToIncidentForm
import com.hyejeanmoon.fieldincidentreporter.feature.list.IncidentListRoute
import com.hyejeanmoon.fieldincidentreporter.feature.list.incidentListScreen
import com.hyejeanmoon.fieldincidentreporter.feature.list.returnToIncidentListAfterMutation

private const val TransitionMillis = 320

@Composable
fun IncidentNavHost(navController: NavHostController = rememberNavController()) {
    val navigateBack: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = IncidentListRoute,
        enterTransition = { slideIn(fromEnd = true) },
        exitTransition = { slideOut(toEnd = false) },
        popEnterTransition = { slideIn(fromEnd = false) },
        popExitTransition = { slideOut(toEnd = true) },
    ) {
        incidentListScreen(
            onReportIncident = { navController.navigateToIncidentForm() },
            onIncidentClick = navController::navigateToIncidentDetail,
        )
        incidentDetailScreen(
            onBack = navigateBack,
            onDeleted = navController::returnToIncidentListAfterMutation,
            onEdit = navController::navigateToIncidentForm,
        )
        incidentFormScreen(
            onBack = navigateBack,
            onCreated = navController::returnToIncidentListAfterMutation,
            // A save already updated the cached list, so only the details need to reload.
            onUpdated = navController::returnToIncidentDetailAfterEdit,
        )
    }
}

private fun slideIn(fromEnd: Boolean): EnterTransition =
    slideInHorizontally(tween(TransitionMillis, easing = FastOutSlowInEasing)) { if (fromEnd) it else -it } +
        fadeIn(tween(TransitionMillis))

private fun slideOut(toEnd: Boolean): ExitTransition =
    slideOutHorizontally(tween(TransitionMillis, easing = FastOutSlowInEasing)) { if (toEnd) it else -it } +
        fadeOut(tween(TransitionMillis))
