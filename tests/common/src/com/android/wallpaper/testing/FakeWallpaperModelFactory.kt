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

package com.android.wallpaper.testing

import android.content.Context
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.util.converter.DefaultWallpaperModelFactory
import com.android.wallpaper.util.converter.WallpaperModelFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test fake for [WallpaperModelFactory] that returns an override value if given, otherwise the
 * conversion given by the real AOSP factory.
 */
@Singleton
class FakeWallpaperModelFactory
@Inject
constructor(private val realFactory: DefaultWallpaperModelFactory) : WallpaperModelFactory {

    private var overrideWallpaperModel: WallpaperModel? = null

    override fun getWallpaperModel(context: Context, wallpaperInfo: WallpaperInfo): WallpaperModel {
        return overrideWallpaperModel ?: realFactory.getWallpaperModel(context, wallpaperInfo)
    }

    fun getOverrideWallpaperModel(): WallpaperModel? {
        return overrideWallpaperModel
    }

    fun setOverrideWallpaperModel(model: WallpaperModel?) {
        overrideWallpaperModel = model
    }
}
