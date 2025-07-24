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

package com.android.wallpaper.picker.wallpapers.view.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.android.wallpaper.picker.wallpapers.domain.interactor.CategoryWallpapers
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [WallpapersViewModel] is responsible for preparing and managing UI-related data for the
 * Wallpapers screen in a lifecycle-aware manner.
 */
@HiltViewModel
class WallpapersViewModel
@Inject
constructor(
    private val categoryWallpapers: CategoryWallpapers,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * This [Flow] emits [WallpapersContentViewModel] by mapping the [List<WallpaperModel>] of a
     * category to [List<WallpapersItemViewModel>]
     */
    private val wallpapersContentViewModel: Flow<WallpapersContentViewModel> =
        categoryWallpapers.selectedCategoryWallpapers.map { wallpapers ->
            val wallpaperItems: List<WallpapersItemViewModel> =
                wallpapers.map {
                    WallpapersItemViewModel.ThumbnailsViewModel(
                        thumbnailAsset = it.commonWallpaperData.thumbAsset,
                        title = it.commonWallpaperData.title,
                        contentDescription = it.commonWallpaperData.title,
                    )
                }

            return@map WallpapersContentViewModel(
                rotationEnabled = false,
                wallpaperItems = wallpaperItems,
            )
        }
}
