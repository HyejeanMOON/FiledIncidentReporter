package com.hyejeanmoon.fieldincidentreporter.feature.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.error.toIncidentFailure
import com.hyejeanmoon.fieldincidentreporter.domain.model.Incident
import com.hyejeanmoon.fieldincidentreporter.ui.component.ErrorContent
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentPhotoThumbnail
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentStatusBadge
import com.hyejeanmoon.fieldincidentreporter.ui.component.LabeledText
import com.hyejeanmoon.fieldincidentreporter.ui.component.LoadingContent
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme
import com.hyejeanmoon.fieldincidentreporter.ui.util.formatIncidentTime
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant

@Composable
internal fun IncidentListScreen(
    refreshRequested: Boolean,
    onRefreshRequestHandled: () -> Unit,
    onReportIncident: () -> Unit,
    onIncidentClick: (String) -> Unit,
    viewModel: IncidentListViewModel = hiltViewModel(),
) {
    val incidents = viewModel.incidents.collectAsLazyPagingItems()
    LaunchedEffect(refreshRequested) {
        if (refreshRequested) {
            incidents.refresh()
            onRefreshRequestHandled()
        }
    }
    // Reports changed elsewhere, such as in the web admin, show up when the app returns to the foreground.
    // The state when the list appears is skipped, since the Pager refreshes on its own then.
    LaunchedEffect(incidents) {
        ProcessLifecycleOwner.get().lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .distinctUntilChanged()
            .drop(1)
            .filter { it }
            .collect { incidents.refresh() }
    }
    IncidentListContent(
        incidents = incidents,
        onReportIncident = onReportIncident,
        onIncidentClick = onIncidentClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IncidentListContent(
    incidents: LazyPagingItems<Incident>,
    onReportIncident: () -> Unit,
    onIncidentClick: (String) -> Unit,
) {
    // Follows the server refresh; Room reloading the list after each write does not change it.
    val refresh = incidents.loadState.refresh
    val refreshFailure = (refresh as? LoadState.Error)?.error?.toIncidentFailure()
    if (refresh is LoadState.Loading && incidents.itemCount == 0) {
        LoadingContent(Modifier.fillMaxSize(), message = stringResource(R.string.loading_incidents))
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = incidents::refresh, enabled = refresh !is LoadState.Loading) {
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
        when {
            refreshFailure != null && incidents.itemCount == 0 -> ErrorContent(
                failure = refreshFailure,
                onRetry = incidents::refresh,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
            incidents.itemCount == 0 -> EmptyIncidentList(
                onReportIncident = onReportIncident,
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
            )
            else -> PullToRefreshBox(
                isRefreshing = refresh is LoadState.Loading,
                onRefresh = incidents::refresh,
                modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 16.dp,
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
                    if (refreshFailure != null) {
                        item {
                            Text(
                                text = stringResource(R.string.cached_incidents_notice),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ErrorContent(failure = refreshFailure, onRetry = incidents::refresh)
                        }
                    }
                    items(count = incidents.itemCount, key = incidents.itemKey(Incident::id)) { index ->
                        incidents[index]?.let { incident ->
                            IncidentCard(incident = incident, onClick = { onIncidentClick(incident.id) })
                        }
                    }
                    when (val append = incidents.loadState.append) {
                        is LoadState.Loading -> item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                        is LoadState.Error -> item {
                            ErrorContent(failure = append.error.toIncidentFailure(), onRetry = incidents::retry)
                        }
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyIncidentList(onReportIncident: () -> Unit, modifier: Modifier = Modifier) {
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
        Button(onClick = onReportIncident) { Text(stringResource(R.string.report_incident)) }
    }
}

@Composable
private fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IncidentStatusBadge(isResolved = incident.isResolved)
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            LabeledText(
                label = stringResource(R.string.location_label),
                value = incident.location,
                labelStyle = MaterialTheme.typography.labelMedium,
                valueStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            LabeledText(
                label = stringResource(R.string.description_label),
                value = incident.description,
                labelStyle = MaterialTheme.typography.labelMedium,
                valueStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
            )
            TimestampText(R.string.created_at_label, incident.createdAt)
            if (incident.isUpdated) TimestampText(R.string.updated_at_label, incident.updatedAt)
            if (incident.photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(incident.photos) { index, photo ->
                        IncidentPhotoThumbnail(
                            uri = photo.thumbnailUri,
                            index = index,
                            modifier = Modifier.size(width = 120.dp, height = 144.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.photo_count, incident.photos.size),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun TimestampText(@StringRes labelRes: Int, time: Instant) {
    Text(
        text = stringResource(labelRes) + ": " + formatIncidentTime(time),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Incident list", showBackground = true)
@Composable
private fun IncidentListPreview() {
    val incident = Incident(
        id = "preview-hydraulic-press",
        title = "Hydraulic press is not responding",
        location = "Workshop · Bay 3",
        description = "The control panel does not respond when the machine is switched on.",
        createdAt = Instant.parse("2026-10-20T09:00:00Z"),
        updatedAt = Instant.parse("2026-10-21T11:30:00Z"),
    )
    FieldIncidentReporterTheme {
        IncidentListContent(
            incidents = flowOf(PagingData.from(listOf(incident))).collectAsLazyPagingItems(),
            onReportIncident = {},
            onIncidentClick = {},
        )
    }
}

@Preview(name = "Empty incident list", showBackground = true)
@Composable
private fun EmptyIncidentListPreview() {
    FieldIncidentReporterTheme {
        IncidentListContent(
            incidents = flowOf(PagingData.from(emptyList<Incident>())).collectAsLazyPagingItems(),
            onReportIncident = {},
            onIncidentClick = {},
        )
    }
}
