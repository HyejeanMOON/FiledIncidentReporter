package com.hyejeanmoon.fieldincidentreporter.feature.incidents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object IncidentDestinations {
    const val List = "incidents"
    const val Detail = "incident/{incidentId}"
    const val Report = "report?incidentId={incidentId}"

    fun detail(id: String) = "incident/$id"
    fun edit(id: String) = "report?incidentId=$id"
}

@Composable
fun IncidentApp(viewModel: IncidentViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = IncidentDestinations.List) {
        composable(IncidentDestinations.List) {
            IncidentListScreen(
                incidents = uiState.incidents,
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
            val incident = uiState.incidents.firstOrNull { it.id == id }
            if (incident == null) {
                MissingIncidentScreen(onBack = { navController.popBackStack() })
            } else {
                IncidentDetailScreen(
                    incident = incident,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(IncidentDestinations.edit(incident.id)) },
                    onDelete = {
                        viewModel.removeIncident(incident.id)
                        navController.popBackStack()
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
        ) { entry ->
            val id = entry.arguments?.getString("incidentId")
            val incident = uiState.incidents.firstOrNull { it.id == id }
            if (id != null && incident == null) {
                MissingIncidentScreen(onBack = { navController.popBackStack() })
            } else {
                IncidentFormRoute(
                    incident = incident,
                    onBack = { navController.popBackStack() },
                    onSave = { title, location, description, photos ->
                        viewModel.saveIncident(id, title, location, description, photos)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}


@Composable
private fun IncidentFormRoute(
    incident: Incident?,
    onBack: () -> Unit,
    onSave: (title: String, location: String, description: String, photos: List<String>) -> Unit,
) {
    var photoUris by rememberSaveable(incident?.id) {
        mutableStateOf(incident?.photoUris.orEmpty())
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            photoUris = selectedUris.map { it.toString() }
        }
    }

    IncidentFormScreen(
        incident = incident,
        photoUris = photoUris,
        onBack = onBack,
        onChoosePhotos = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRemovePhotos = { photoUris = emptyList() },
        onSave = onSave,
    )
}
