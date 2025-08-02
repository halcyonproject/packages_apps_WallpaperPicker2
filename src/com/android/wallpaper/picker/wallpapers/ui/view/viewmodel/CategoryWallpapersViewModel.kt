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

package com.android.wallpaper.picker.wallpapers.ui.view.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.android.wallpaper.picker.wallpapers.domain.interactor.CategoryWallpapersInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [CategoryWallpapersViewModel] is responsible for preparing and managing UI-related data for the
 * Wallpapers screen in a lifecycle-aware manner.
 */
@HiltViewModel
class CategoryWallpapersViewModel
@Inject
constructor(
    private val categoryWallpapersInteractor: CategoryWallpapersInteractor,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * This [Flow] emits [CategoryWallpapersContentViewModel] by mapping the [List<WallpaperModel>]
     * of a category to [List<CategoryWallpapersItemViewModel>]
     */
    val categoryWallpapersContentViewModel: Flow<CategoryWallpapersContentViewModel> =
        categoryWallpapersInteractor.selectedCategoryWallpapers.map { wallpapers ->
            val wallpaperItems =
                wallpapers.map {
                    CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory(
                        thumbnailAsset = it.commonWallpaperData.thumbAsset,
                        title = it.commonWallpaperData.title,
                        contentDescription = it.commonWallpaperData.title,
                    )
                }

            // this is just a placeholder to test the view layer
            return@map CategoryWallpapersContentViewModel(
                rotationEnabled = false,
                wallpaperItems =
                    listOf(
                        CategoryWallpapersItemViewModel.TemplateThumbnailsViewModelCategory(
                            wallpaperItems
                        ),
                        CategoryWallpapersItemViewModel.PlainThumbnailsViewModelCategory(
                            wallpaperItems
                        ),
                    ),
            )
        }
}
