/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.intentresolver.contentpreview.payloadtoggle.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.intentresolver.R
import com.android.intentresolver.contentpreview.payloadtoggle.shared.model.PreviewsModel
import com.android.intentresolver.contentpreview.payloadtoggle.ui.viewmodel.ContentType
import com.android.intentresolver.contentpreview.payloadtoggle.ui.viewmodel.ShareouselPreviewViewModel
import com.android.intentresolver.contentpreview.payloadtoggle.ui.viewmodel.ShareouselViewModel
import kotlinx.coroutines.launch

@Composable
fun Shareousel(viewModel: ShareouselViewModel) {
    val keySet = viewModel.previews.collectAsStateWithLifecycle(null).value
    if (keySet != null) {
        Shareousel(viewModel, keySet)
    } else {
        Spacer(
            Modifier.height(dimensionResource(R.dimen.chooser_preview_image_height_tall) + 64.dp)
        )
    }
}

@Composable
private fun Shareousel(viewModel: ShareouselViewModel, keySet: PreviewsModel) {
    Column(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 16.dp),
    ) {
        PreviewCarousel(keySet, viewModel)
        ActionCarousel(viewModel)
    }
}

@Composable
private fun PreviewCarousel(
    previews: PreviewsModel,
    viewModel: ShareouselViewModel,
) {
    val centerIdx = previews.startIdx
    val carouselState = rememberLazyListState(initialFirstVisibleItemIndex = centerIdx)
    // TODO: start item needs to be centered, check out ScalingLazyColumn impl or see if
    //  HorizontalPager works for our use-case
    LazyRow(
        state = carouselState,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier.fillMaxWidth()
                .height(dimensionResource(R.dimen.chooser_preview_image_height_tall))
    ) {
        items(previews.previewModels.toList(), key = { it.uri }) { model ->
            ShareouselCard(viewModel.preview(model))
        }
    }
}

@Composable
private fun ShareouselCard(viewModel: ShareouselPreviewViewModel) {
    val bitmap by viewModel.bitmap.collectAsStateWithLifecycle(initialValue = null)
    val selected by viewModel.isSelected.collectAsStateWithLifecycle(initialValue = false)
    val contentType by
        viewModel.contentType.collectAsStateWithLifecycle(initialValue = ContentType.Image)
    val borderColor = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    ShareouselCard(
        image = {
            bitmap?.let { bitmap ->
                val aspectRatio =
                    (bitmap.width.toFloat() / bitmap.height.toFloat())
                        // TODO: max ratio is actually equal to the viewport ratio
                        .coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(aspectRatio),
                )
            }
                ?: run {
                    // TODO: look at ScrollableImagePreviewView.setLoading()
                    Box(
                        modifier =
                            Modifier.fillMaxHeight()
                                .aspectRatio(2f / 5f)
                                .border(1.dp, Color.Red, RectangleShape)
                    )
                }
        },
        contentType = contentType,
        selected = selected,
        modifier =
            Modifier.thenIf(selected) {
                    Modifier.border(
                        width = 4.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(size = 12.dp),
                    )
                }
                .clip(RoundedCornerShape(size = 12.dp))
                .clickable { scope.launch { viewModel.setSelected(!selected) } },
    )
}

@Composable
private fun ActionCarousel(viewModel: ShareouselViewModel) {
    val actions by viewModel.actions.collectAsStateWithLifecycle(initialValue = emptyList())
    if (actions.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(32.dp),
        ) {
            itemsIndexed(actions) { idx, actionViewModel ->
                if (idx == 0) {
                    Spacer(Modifier.width(dimensionResource(R.dimen.chooser_edge_margin_normal)))
                }
                ShareouselAction(
                    label = actionViewModel.label,
                    onClick = { actionViewModel.onClicked() },
                ) {
                    actionViewModel.icon?.let { Image(icon = it, modifier = Modifier.size(16.dp)) }
                }
                if (idx == actions.size - 1) {
                    Spacer(Modifier.width(dimensionResource(R.dimen.chooser_edge_margin_normal)))
                }
            }
        }
    }
}

@Composable
private fun ShareouselAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        modifier = modifier
    )
}

inline fun Modifier.thenIf(condition: Boolean, crossinline factory: () -> Modifier): Modifier =
    if (condition) this.then(factory()) else this

private const val MIN_ASPECT_RATIO = 0.4f
private const val MAX_ASPECT_RATIO = 2.5f
