package com.hyejeanmoon.fieldincidentreporter.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import com.hyejeanmoon.fieldincidentreporter.R

@Composable
fun IncidentPhotoImage(
    uri: String,
    index: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fullResolution: Boolean = false,
) {
    val context = LocalContext.current
    val model = remember(uri, fullResolution, context) {
        if (fullResolution) {
            ImageRequest.Builder(context)
                .data(uri)
                .size(Size.ORIGINAL)
                .precision(Precision.EXACT)
                .memoryCacheKeyExtra("resolution", "original")
                .build()
        } else {
            uri
        }
    }
    AsyncImage(
        model = model,
        contentDescription = stringResource(R.string.incident_photo, index + 1),
        modifier = modifier,
        contentScale = contentScale,
        filterQuality = if (fullResolution) FilterQuality.High else FilterQuality.Low,
    )
}
