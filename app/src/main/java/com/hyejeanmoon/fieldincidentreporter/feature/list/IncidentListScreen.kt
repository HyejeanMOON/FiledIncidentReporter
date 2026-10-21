package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.feature.common.LoadingIncidentScreen
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    incidents: List<Incident>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onReportIncident: () -> Unit,
    onIncidentClick: (Incident) -> Unit,
) {
    if (isLoading) {
        LoadingIncidentScreen(message = stringResource(R.string.loading_incidents))
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text(stringResource(R.string.refresh))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onReportIncident) {
                Text(stringResource(R.string.report_incident))
            }
        },
    ) { contentPadding ->
        if (incidents.isEmpty()) {
            EmptyIncidentList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                onReportIncident = onReportIncident,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 16.dp + contentPadding.calculateTopPadding(),
                    end = 20.dp,
                    bottom = 96.dp + contentPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.incident_list_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(incidents, key = { it.id }) { incident ->
                    IncidentCard(incident = incident, onClick = { onIncidentClick(incident) })
                }
            }
        }
    }
}

@Composable
private fun EmptyIncidentList(modifier: Modifier = Modifier, onReportIncident: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_list_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.empty_list_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onReportIncident) {
            Text(stringResource(R.string.report_incident))
        }
    }
}

@Composable
private fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = incident.location,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (incident.photoUris.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.photo_count, incident.photoUris.size),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private val PreviewIncident = Incident(
    id = "preview-hydraulic-press",
    title = "Hydraulic press is not responding",
    location = "Workshop · Bay 3",
    description = "The control panel does not respond when the machine is switched on.",
)

@Preview(name = "Incident list", showBackground = true)
@Composable
private fun IncidentListScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentListScreen(
            incidents = listOf(PreviewIncident),
            isLoading = false,
            onRefresh = {},
            onReportIncident = {},
            onIncidentClick = {},
        )
    }
}

@Preview(name = "Empty incident list", showBackground = true)
@Composable
private fun EmptyIncidentListScreenPreview() {
    FieldIncidentReporterTheme {
        IncidentListScreen(
            incidents = emptyList(),
            isLoading = false,
            onRefresh = {},
            onReportIncident = {},
            onIncidentClick = {},
        )
    }
}
