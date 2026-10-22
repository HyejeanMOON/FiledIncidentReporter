package com.hyejeanmoon.fieldincidentreporter.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.hyejeanmoon.fieldincidentreporter.R

@Composable
fun IncidentPhotoImage(
    uri: String,
    index: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = uri,
        contentDescription = stringResource(R.string.incident_photo, index + 1),
        modifier = modifier,
        contentScale = contentScale,
    )
}
