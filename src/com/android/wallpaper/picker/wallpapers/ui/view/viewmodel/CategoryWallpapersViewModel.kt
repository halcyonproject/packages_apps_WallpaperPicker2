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
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
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
    private val persistentWallpaperModelRepository: PersistentWallpaperModelRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * This [Flow] emits [CategoryWallpapersContentViewModel] by mapping the [List<WallpaperModel>]
     * of a category to [List<CategoryWallpapersItemViewModel>]
     */
    val categoryWallpapersContentViewModel: Flow<CategoryWallpapersContentViewModel> =
        categoryWallpapersInteractor.selectedCategoryWallpapers.map { wallpapers ->
            if (DEBUG) {
                Log.d(TAG, "WallpaperModels: ${wallpapers.size}")
            }

            val groupedWallpapers =
                wallpapers
                    .groupBy { wallpaper ->
                        val groupName =
                            when (wallpaper) {
                                is WallpaperModel.LiveWallpaperModel ->
                                    wallpaper.liveWallpaperData.groupName
                                is WallpaperModel.StaticWallpaperModel ->
                                    wallpaper.downloadableWallpaperData?.groupName
                                else -> null
                            }

                        if (groupName.isNullOrEmpty()) {
                            DEFAULT_GROUP
                        } else {
                            groupName
                        }
                    }
                    .toMutableMap()

            if (DEBUG) {
                Log.d(TAG, "Wallpaper groups: ")
                for ((groupName, wallpapers) in groupedWallpapers) {
                    Log.d(TAG, "Group NAME: ${groupName}")
                    for (wallpaper in wallpapers) {
                        Log.d(TAG, "${wallpaper}")
                    }
                }
            }

            val firstEntry = groupedWallpapers.keys.firstOrNull()

            val templates = buildList {
                if (
                    firstEntry != null &&
                        firstEntry != DEFAULT_GROUP &&
                        (groupedWallpapers[firstEntry]?.get(0)
                            is WallpaperModel.LiveWallpaperModel) &&
                        (groupedWallpapers[firstEntry]?.get(0)
                                as? WallpaperModel.LiveWallpaperModel)
                            ?.creativeWallpaperData != null &&
                        (groupedWallpapers[firstEntry]?.size ?: 0) > 1
                ) {
                    add(CategoryWallpapersItemViewModel.PrimaryHeaderViewModelCategory(firstEntry))
                    val items =
                        groupedWallpapers[firstEntry]?.map {
                            CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory(
                                thumbnailAsset = it.commonWallpaperData.thumbAsset,
                                title = it.commonWallpaperData.title,
                                contentDescription = it.commonWallpaperData.title,
                                onSectionClicked = {
                                    persistentWallpaperModelRepository.setWallpaperModel(it)
                                    val previewIntent =
                                        WallpaperPreviewActivity.intentBuilder(context, true)
                                            .refreshCategory(true)
                                            .navigateToExtendedEffects(false)
                                            .build()
                                    return@ThumbnailsViewModelCategory previewIntent
                                },
                            )
                        }
                    items?.let {
                        add(CategoryWallpapersItemViewModel.TemplateThumbnailsViewModelCategory(it))
                        groupedWallpapers.remove(firstEntry)
                    }
                }
            }

            val wallpaperItems = buildList {
                for ((groupName, wallpapers) in groupedWallpapers) {
                    if (groupName != DEFAULT_GROUP) {
                        add(
                            CategoryWallpapersItemViewModel.SecondaryHeaderViewModelCategory(
                                groupName
                            )
                        )
                    }
                    val items =
                        wallpapers.map {
                            CategoryWallpapersItemViewModel.ThumbnailsViewModelCategory(
                                thumbnailAsset = it.commonWallpaperData.thumbAsset,
                                title = it.commonWallpaperData.title,
                                contentDescription = it.commonWallpaperData.title,
                                onSectionClicked = {
                                    persistentWallpaperModelRepository.setWallpaperModel(it)
                                    val previewIntent =
                                        WallpaperPreviewActivity.intentBuilder(context, true)
                                            .refreshCategory(true)
                                            .navigateToExtendedEffects(false)
                                            .build()
                                    return@ThumbnailsViewModelCategory previewIntent
                                },
                            )
                        }
                    add(CategoryWallpapersItemViewModel.PlainThumbnailsViewModelCategory(items))
                }
            }

            if (DEBUG) {
                Log.d(TAG, "here is the list of wallpaperItems yo: ${wallpaperItems}")
            }

            return@map CategoryWallpapersContentViewModel(
                rotationEnabled = false,
                wallpaperItems = templates + wallpaperItems,
            )
        }

    companion object {
        const val DEBUG = false
        const val TAG = "CategoryWallpapersViewModel"
        const val DEFAULT_GROUP = "default_group"
    }
}
