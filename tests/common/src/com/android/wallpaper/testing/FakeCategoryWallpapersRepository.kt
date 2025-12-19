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

package com.android.wallpaper.testing

import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.wallpapers.data.repository.CategoryWallpapersRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class FakeCategoryWallpapersRepository @Inject constructor() : CategoryWallpapersRepository {

    var invalidateCacheFunctionCallCount = 0

    var refreshWallpapersFunctionCallCount = 0

    override val selectedCategoryModel: StateFlow<CategoryModel?> = MutableStateFlow(null)

    override val isWallpapersFetching: StateFlow<Boolean> = MutableStateFlow(false)

    override val selectedCategoryWallpapers: StateFlow<List<WallpaperModel>> =
        MutableStateFlow(emptyList())

    override fun setSelectedCategory(category: CategoryModel) {}

    override fun refreshWallpapers() {
        refreshWallpapersFunctionCallCount++
    }

    override fun clearSelectedCategory() {}

    override suspend fun startRotation(networkPreference: Int) {}

    override fun invalidateCache(categoryId: String) {
        invalidateCacheFunctionCallCount++
    }
}
