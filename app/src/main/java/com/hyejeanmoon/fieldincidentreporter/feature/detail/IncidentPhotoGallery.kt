package com.hyejeanmoon.fieldincidentreporter.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyejeanmoon.fieldincidentreporter.R
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentPhotoImage
import com.hyejeanmoon.fieldincidentreporter.ui.component.IncidentPhotoViewer
import kotlinx.coroutines.launch

@Composable
internal fun IncidentPhotoGallery(photoUrls: List<String>) {
    if (photoUrls.isEmpty()) return

    var viewerPage by rememberSaveable(photoUrls) { mutableIntStateOf(-1) }
    val pagerState = rememberPagerState(pageCount = { photoUrls.size })
    val coroutineScope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(260.dp),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.view_full_photo),
                    ) { viewerPage = page },
                contentAlignment = Alignment.Center,
            ) {
                IncidentPhotoImage(
                    uri = photoUrls[page],
                    index = page,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = stringResource(R.string.photo_page_count, pagerState.currentPage + 1, photoUrls.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (viewerPage >= 0) {
        IncidentPhotoViewer(
            photoUris = photoUrls,
            initialPage = viewerPage,
            onDismiss = { page ->
                viewerPage = -1
                coroutineScope.launch { pagerState.scrollToPage(page) }
            },
        )
    }
}
