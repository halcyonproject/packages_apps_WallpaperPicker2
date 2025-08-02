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

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.android.wallpaper.R
import com.android.wallpaper.picker.wallpapers.ui.view.viewmodel.CategoryWallpapersContentViewModel
import com.android.wallpaper.picker.wallpapers.ui.view.viewmodel.CategoryWallpapersItemViewModel
import com.android.wallpaper.util.SizeCalculator

/** Scale factor used to calculate the height of template tiles */
private const val THUMBNAIL_TILE_HEIGHT_SCALE_FACTOR: Float = 1.2f

/**
 * Displays the main screen content for category wallpapers using a [LazyColumn].
 *
 * @param viewModel The [CategoryWallpapersContentViewModel] providing the wallpaper items to
 *   render.
 */
@Composable
fun WallpapersScreenContent(
    viewModel: CategoryWallpapersContentViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(viewModel.wallpaperItems) { item ->
            when (item) {
                is CategoryWallpapersItemViewModel.PrimaryHeaderViewModelCategory -> {
                    Text(
                        text = item.title,
                        modifier =
                            modifier.padding(dimensionResource(R.dimen.category_grid_edge_space)),
                    )
                }

                is CategoryWallpapersItemViewModel.SecondaryHeaderViewModelCategory -> {
                    Text(
                        text = item.title,
                        modifier =
                            modifier.padding(
                                horizontal =
                                    dimensionResource(
                                        R.dimen.grid_item_individual_padding_horizontal
                                    ),
                                vertical =
                                    dimensionResource(R.dimen.grid_item_individual_padding_bottom),
                            ),
                    )
                }

                is CategoryWallpapersItemViewModel.TemplateThumbnailsViewModelCategory -> {
                    HorizontalGridSection(thumbnails = item.thumbnailAssets, maxRows = 2)
                }

                is CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory -> {
                    ThumbnailCard(item)
                }

                is CategoryWallpapersItemViewModel.PlainThumbnailsViewModelCategory -> {
                    ThumbnailGridSection(thumbnails = item.thumbnailAssets, columns = 3)
                }
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
    maxRows: Int,
    modifier: Modifier = Modifier,
) {
    val density: Density = LocalDensity.current
    val tileHeightPx: Float =
        LocalActivity.current?.let { SizeCalculator.getFeaturedIndividualTileSize(it).y.toFloat() }
            ?: with(density) { 260.dp.toPx() }

    val heightDp =
        with(density) { (THUMBNAIL_TILE_HEIGHT_SCALE_FACTOR * tileHeightPx).toInt().toDp() }
    LazyHorizontalGrid(
        rows = GridCells.Fixed(maxRows),
        modifier =
            modifier
                .fillMaxWidth()
                .height(
                    heightDp * 2
                ) // need to set a fixed height for this grid as its inside of a LazyColumn
                .padding(
                    vertical =
                        dimensionResource(R.dimen.creative_category_individual_item_view_space)
                ),
        horizontalArrangement =
            Arrangement.spacedBy(
                dimensionResource(R.dimen.creative_category_individual_item_view_space)
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                dimensionResource(R.dimen.creative_category_individual_item_view_space)
            ),
        contentPadding =
            PaddingValues(
                horizontal = dimensionResource(R.dimen.featured_wallpaper_grid_edge_space)
            ),
    ) {
        items(thumbnails) { thumbnail -> ThumbnailCard(thumbnail) }
    }
}

/**
 * Displays a grid of thumbnails arranged in a fixed number of columns.
 *
 * @param thumbnails List of thumbnail view models to display.
 * @param columns Number of columns in the grid.
 */
@Composable
fun ThumbnailGridSection(
    thumbnails: List<CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory>,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    val rows = (thumbnails.size + columns - 1) / columns

    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (rowIndex in 0 until rows) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (colIndex in 0 until columns) {
                    val itemIndex = rowIndex * columns + colIndex
                    if (itemIndex < thumbnails.size) {
                        ThumbnailCard(thumbnails[itemIndex])
                    }
                }
            }
        }
    }
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.size(120.dp).clickable { thumbnail.onSectionClicked?.invoke() },
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.grid_item_all_radius_small)),
        elevation = CardDefaults.cardElevation(),
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Replace this with an image loader
            Text(
                text = thumbnail.title ?: stringResource(R.string.default_wallpaper_title),
                modifier = modifier.align(Alignment.BottomStart).padding(8.dp),
                color = Color.White,
            )
        }
    }
}
