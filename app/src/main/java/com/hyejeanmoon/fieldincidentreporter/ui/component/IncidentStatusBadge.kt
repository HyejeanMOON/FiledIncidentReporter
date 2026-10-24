package com.hyejeanmoon.fieldincidentreporter.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyejeanmoon.fieldincidentreporter.R

/** Whether an operator has resolved the report yet. */
@Composable
fun IncidentStatusBadge(isResolved: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isResolved) colors.secondaryContainer else colors.tertiaryContainer,
        contentColor = if (isResolved) colors.onSecondaryContainer else colors.onTertiaryContainer,
    ) {
        Text(
            text = stringResource(if (isResolved) R.string.status_resolved else R.string.status_open),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
