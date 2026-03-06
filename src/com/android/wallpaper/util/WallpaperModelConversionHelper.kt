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

package com.android.wallpaper.util

import android.app.WallpaperInfo
import android.app.wallpaper.WallpaperDescription
import android.content.Context
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.picker.data.ImageWallpaperData
import com.android.wallpaper.picker.data.InternalLiveWallpaperData
import com.android.wallpaper.picker.data.WallpaperModel

/**
 * This provides a factory method to generate relevant [WallpaperModel] fields when we convert
 * [WallpaperDescription] or [WallpaperInfo] to [WallpaperModel].
 */
interface WallpaperModelConversionHelper {
    fun getLiveWallpaperThumbAssets(context: Context, wallpaperInfo: WallpaperInfo): Asset

    /** Returns true if the wallpaper is a creative wallpaper. */
    fun isCreative(wallpaperInfo: WallpaperInfo): Boolean

    /** Creates the [InternalLiveWallpaperData] for the given [WallpaperModel]. */
    fun getInternalLiveWallpaperData(wallpaperInfo: WallpaperInfo): InternalLiveWallpaperData?

    /** Creates the [ImageWallpaperData] for the given [WallpaperModel]. */
    fun getImageWallpaperData(wallpaperDescription: WallpaperDescription): ImageWallpaperData?
}
