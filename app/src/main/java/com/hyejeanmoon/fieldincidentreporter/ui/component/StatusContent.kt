package com.hyejeanmoon.fieldincidentreporter.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.domain.error.IncidentFailure
import com.hyejeanmoon.fieldincidentreporter.ui.util.messageResId

@Composable
fun LoadingContent(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        message?.let { Text(text = it, modifier = Modifier.padding(top = 12.dp)) }
    }
}

/** A failure message with an optional Retry button and any extra [actions] below it. */
@Composable
fun ErrorContent(
    failure: IncidentFailure,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(failure.messageResId()),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        onRetry?.let { retry ->
            Button(onClick = retry, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
        actions()
    }
}

/** Full screen shown when a report cannot be loaded; a missing report cannot be retried. */
@Composable
fun IncidentLoadFailureScreen(
    title: String,
    failure: IncidentFailure,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(topBar = { BackTopAppBar(title = title, onBack = onBack) }) { contentPadding ->
        ErrorContent(
            failure = failure,
            onRetry = onRetry.takeIf { failure != IncidentFailure.NOT_FOUND },
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_list)) }
        }
    }
}
