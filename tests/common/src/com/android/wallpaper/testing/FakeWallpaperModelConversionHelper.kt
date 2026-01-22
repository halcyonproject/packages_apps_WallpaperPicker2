/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.app.WallpaperInfo
import android.content.Context
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.picker.data.InternalLiveWallpaperData
import com.android.wallpaper.util.WallpaperModelConversionHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeWallpaperModelConversionHelper @Inject constructor() : WallpaperModelConversionHelper {

    var isCreative: Boolean = false

    override fun getLiveWallpaperThumbAssets(
        context: Context,
        wallpaperInfo: WallpaperInfo,
    ): Asset {
        return TestAsset(COLOR_DEFAULT, /* isCorrupt= */ false)
    }

    override fun isCreative(wallpaperInfo: WallpaperInfo): Boolean {
        return isCreative
    }

    override fun getInternalLiveWallpaperData(
        wallpaperInfo: WallpaperInfo
    ): InternalLiveWallpaperData? {
        return null
    }

    companion object {
        private const val COLOR_DEFAULT: Int = 0xff000000.toInt()
    }
}
