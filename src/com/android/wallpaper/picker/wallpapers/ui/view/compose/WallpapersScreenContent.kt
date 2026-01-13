/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.picker.wallpapers.ui.view.compose

import android.app.Activity
import android.graphics.Point
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.compose.PlatformOutlinedButton
import com.android.compose.PlatformTextButton
import com.android.compose.ui.graphics.painter.rememberDrawablePainter
import com.android.wallpaper.R
import com.android.wallpaper.picker.wallpapers.ui.view.viewmodel.CategoryWallpapersContentViewModel
import com.android.wallpaper.picker.wallpapers.ui.view.viewmodel.CategoryWallpapersItemViewModel
import com.android.wallpaper.util.ResourceUtils
import com.android.wallpaper.util.SizeCalculator
import kotlinx.coroutines.launch

/** Scale factor used to calculate the height of template tiles */
private const val TILE_HEIGHT_SCALE_FACTOR: Float = 1.2f

/** Scale factor used to calculate the width of template tiles */
private const val TILE_WIDTH_SCALE_FACTOR: Float = 0.95f

private const val TAG = "WallpapersScreenContent"

private const val THUMBNAIL_TEST_TAG = "wp_thumbnail"

/**
 * Displays the main screen content for category wallpapers using a [LazyColumn].
 *
 * @param viewModel The [CategoryWallpapersContentViewModel] providing the wallpaper items to
 *   render.
 */
@Composable
fun WallpapersScreenContent(
    viewModel: CategoryWallpapersContentViewModel,
    isRotationDialogShowing: Boolean,
    isRotationLoading: Boolean,
    networkPreference: Int,
    modifier: Modifier = Modifier,
) {

    var expanded by remember { mutableStateOf(true) } // initial state (expanded)

    val activity = LocalActivity.current
    val featuredTileSize: DpSize =
        activity?.let { SizeCalculator.getFeaturedIndividualTileSize(it) }?.toDpSize()
            ?: DpSize(0.dp, 0.dp)
    val regularTileSize: DpSize =
        activity?.let { SizeCalculator.getIndividualTileSize(it) }?.toDpSize() ?: DpSize(0.dp, 0.dp)

    val horizontalPadding = dimensionResource(R.dimen.grid_item_start)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                )
                // This semantics modifier is used to make testTags act like resource IDs,
                // which allows UI automation tools like Espresso to find elements using their
                // testTag.
                .semantics { testTagsAsResourceId = true }
    ) {
        TopToolbar(
            viewModel.title,
            viewModel.rotationEnabled,
            viewModel.onShowRotationDialog,
            modifier = modifier,
        )
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(viewModel.wallpaperItems) { item ->
                when (item) {
                    is CategoryWallpapersItemViewModel.PrimaryHeaderViewModelCategory -> {
                        SectionLabel(
                            item.title,
                            modifier.wrapContentSize().padding(start = horizontalPadding),
                        )
                    }

                    is CategoryWallpapersItemViewModel.SecondaryHeaderViewModelCategory -> {
                        if (viewModel.isNewUIEnabled) {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable { expanded = !expanded }
                                        .padding(start = horizontalPadding, end = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val colorScheme = MaterialTheme.colorScheme
                                Text(
                                    text = item.title,
                                    modifier = modifier,
                                    fontSize = 16.sp,
                                    lineHeight = 15.sp,
                                    color = colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.W500,
                                )

                                Icon(
                                    imageVector =
                                        ImageVector.vectorResource(
                                            R.drawable.ic_arrow_forward_24px
                                        ),
                                    contentDescription =
                                        if (expanded)
                                            stringResource(R.string.collapse_content_description)
                                        else stringResource(R.string.expand_content_description),
                                    modifier =
                                        Modifier.rotate(if (expanded) 90f else 0f)
                                            .animateContentSize(),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else {
                            SectionLabel(
                                item.title,
                                modifier.wrapContentSize().padding(start = horizontalPadding),
                            )
                        }
                    }

                    is CategoryWallpapersItemViewModel.TemplateThumbnailsViewModelCategory -> {
                        HorizontalGridSection(
                            thumbnails = item.thumbnailAssets,
                            viewModel = viewModel,
                            maxRows = 2,
                            contentPadding =
                                PaddingValues(
                                    horizontal = horizontalPadding,
                                    vertical =
                                        dimensionResource(
                                            R.dimen.creative_category_individual_item_view_space
                                        ),
                                ),
                        )
                    }

                    is CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory -> {
                        ThumbnailCard(item, viewModel = viewModel)
                    }

                    is CategoryWallpapersItemViewModel.PlainThumbnailsViewModelCategory -> {
                        val tileSize = if (item.areTilesLarge) featuredTileSize else regularTileSize
                        val columnCount =
                            if (item.areTilesLarge) {
                                2
                            } else {
                                activity?.let { SizeCalculator.getNumIndividualColumns(it) } ?: 3
                            }
                        item.thumbnails.chunked(columnCount).forEach { row ->
                            AnimatedVisibility(visible = expanded) {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(
                                                horizontal = horizontalPadding,
                                                vertical = 8.dp,
                                            ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    row.forEach { thumb ->
                                        ThumbnailCard(
                                            thumb,
                                            viewModel = viewModel,
                                            modifier = Modifier.size(tileSize),
                                        )
                                    }

                                    // Add spacers to align the last row
                                    repeat(columnCount - row.size) {
                                        Spacer(modifier = Modifier.size(tileSize))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isRotationDialogShowing) {
        val colorScheme = MaterialTheme.colorScheme
        AlertDialog(
            shape = RoundedCornerShape(24.dp),
            textContentColor = colorScheme.onSurface,
            title = {
                Text(
                    text = stringResource(R.string.start_rotation_dialog_body),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            onDismissRequest = { viewModel.onCancelRotationDialog?.invoke() },
            text = {
                if (isRotationLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = modifier.fillMaxWidth(),
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = modifier.fillMaxWidth().offset(x = -24.dp),
                    ) {
                        Checkbox(
                            checked = networkPreference != 0,
                            onCheckedChange = viewModel.onNetworkPreferences,
                            modifier = modifier.clearAndSetSemantics {},
                        )

                        Text(
                            text =
                                stringResource(
                                    R.string.start_rotation_dialog_wifi_only_option_message
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                if (!isRotationLoading) {
                    PlatformTextButton(onClick = { viewModel.onRotationStart?.invoke() }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            },
            dismissButton = {
                if (!isRotationLoading) {
                    PlatformOutlinedButton(
                        onClick = { viewModel.onCancelRotationDialog?.invoke() }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            },
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = text,
        modifier = modifier,
        fontSize = 16.sp,
        lineHeight = 15.sp,
        color = colorScheme.onSurface,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.W500,
    )
}

@Composable
fun TopToolbar(
    title: String,
    isRotationEnabled: Boolean,
    startRotation: (() -> Unit)?,
    modifier: Modifier,
) {
    val activity = LocalActivity.current as ComponentActivity
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier.clickable { /* Handle back */ }
                    .padding(vertical = 4.dp)
                    .padding(end = 16.dp)
        ) {
            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = { activity.onBackPressedDispatcher.onBackPressed() },
                colors =
                    IconButtonDefaults.iconButtonColors()
                        .copy(containerColor = colorScheme.surfaceContainerHighest),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_nav_back_24dp),
                    contentDescription = stringResource(R.string.bottom_action_bar_back),
                    tint = colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            fontSize = 20.sp,
            color = colorScheme.onSurface,
        )
        if (isRotationEnabled) {
            IconButton(onClick = { startRotation?.invoke() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_slideshow_24dp),
                    contentDescription = "Slideshow",
                    tint = colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Displays a horizontal scrolling grid of wallpaper thumbnails with a fixed number of rows.
 *
 * This grid is intended to be used inside a [LazyColumn], and hence requires an explicitly defined
 * height based on tile size.
 *
 * @param thumbnails List of thumbnail view models to display.
 * @param maxRows Maximum number of rows in the horizontal grid.
 */
@Composable
fun HorizontalGridSection(
    thumbnails: List<CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory>,
    viewModel: CategoryWallpapersContentViewModel,
    maxRows: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val density: Density = LocalDensity.current
    val tileHeightPx: Float =
        LocalActivity.current?.let { SizeCalculator.getFeaturedIndividualTileSize(it).y.toFloat() }
            ?: with(density) { 260.dp.toPx() }
    val heightDp = with(density) { (TILE_HEIGHT_SCALE_FACTOR * tileHeightPx).toInt().toDp() }
    val widthDp = with(density) { (TILE_WIDTH_SCALE_FACTOR * tileHeightPx).toInt().toDp() }

    LazyHorizontalGrid(
        rows = GridCells.Fixed(maxRows),
        modifier = modifier.fillMaxWidth().height(heightDp * 2),
        horizontalArrangement =
            Arrangement.spacedBy(
                dimensionResource(R.dimen.creative_category_individual_item_view_space)
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                dimensionResource(R.dimen.creative_category_individual_item_view_space)
            ),
        contentPadding = contentPadding,
    ) {
        items(thumbnails) { thumbnail ->
            ThumbnailCard(
                thumbnail = thumbnail,
                viewModel = viewModel,
                modifier = modifier.size(width = widthDp, height = heightDp),
                showLabel = true,
            )
        }
    }
}

@Composable
fun Point.toDpSize(): DpSize {
    val density = LocalDensity.current

    val widthInDp = with(density) { this@toDpSize.x.toDp() }
    val heightInDp = with(density) { this@toDpSize.y.toDp() }

    return DpSize(width = widthInDp, height = heightInDp)
}

/**
 * Renders a single wallpaper thumbnail card with a title and click interaction. This will
 * eventually support an image also
 *
 * @param thumbnail The thumbnail view model containing metadata and click behavior.
 */
@Composable
fun ThumbnailCard(
    thumbnail: CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory,
    viewModel: CategoryWallpapersContentViewModel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
) {
    Card(
        modifier = modifier.testTag(THUMBNAIL_TEST_TAG),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.grid_item_all_radius_small)),
        elevation = CardDefaults.cardElevation(),
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            AssetImageView(
                thumbnail = thumbnail,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
            if (showLabel) {
                if (viewModel.isNewUIEnabled) {
                    Text(
                        text = thumbnail.title ?: stringResource(R.string.default_wallpaper_title),
                        modifier =
                            Modifier.align(Alignment.Center)
                                .clip(RoundedCornerShape(100))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    Text(
                        text = thumbnail.title ?: stringResource(R.string.default_wallpaper_title),
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                        color = Color.White,
                    )
                }
            }
            if (thumbnail.isDownloadable) {
                LayerListImage(
                    id = R.drawable.ic_download_badge,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                )
            } else if (thumbnail.isApplied) {
                LayerListImage(
                    id = R.drawable.wallpaper_check_circle_24dp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                )
            }
        }
    }
}

@Composable
fun LayerListImage(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val drawable = AppCompatResources.getDrawable(context, id)

    drawable?.let {
        Image(
            painter = rememberDrawablePainter(drawable = it),
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

/**
 * A circular progress indicator. It appears immediately when `isLoading` is true and disappears
 * immediately when `isLoading` is false.
 *
 * @param isLoading A boolean state indicating whether content is currently loading.
 * @param modifier The modifier to be applied to the loading indicator.
 */
@Composable
fun LoadingSpinner(isLoading: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        }
    }
}

// TODO(b/441293395): Deprecate Asset class and migrate image loading to use GlideImageView
@Composable
fun AssetImageView(
    thumbnail: CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory,
    viewModel: CategoryWallpapersContentViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    coroutineScope.launch { viewModel.dismissScreen() }
                }
            },
        )

    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        },
        update = { imageView ->
            thumbnail.thumbnailAsset.loadDrawable(
                context,
                imageView,
                ResourceUtils.getColorAttr(context, android.R.attr.colorSecondary),
            )
        },
        modifier =
            modifier.clickable {
                val intent = thumbnail.getLaunchActivityIntent?.invoke()
                intent?.let { launcher.launch(it) }
            },
    )
}
