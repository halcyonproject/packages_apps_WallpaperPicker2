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

package com.android.wallpaper.util

import android.content.Context
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.CurrentWallpaperInfoFactory
import com.android.wallpaper.module.InjectorProvider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Utils for [CurrentWallpaperInfo].
 */
@Deprecated("Migrating to CurrentWallpaperModelUtils. See b/448461608")
object CurrentWallpaperInfoUtils {

    /**
     * Determines WallpaperInfo objects representing the currently set wallpaper(s), retrieving the
     * wallpaper id from local metadata if necessary. Updates the current recents key(s) and returns
     * the WallpaperInfo pair, one for the lock screen and one for the home screen.
     */
    suspend fun getCurrentWallpapers(
        context: Context,
        forceRefresh: Boolean,
    ): Pair<WallpaperInfo, WallpaperInfo> = suspendCoroutine { continuation ->
        val injector = InjectorProvider.getInjector()
        val currentWallpaperFactory = injector.getCurrentWallpaperInfoFactory(context)
        currentWallpaperFactory.createCurrentWallpaperInfos(
            context,
            forceRefresh,
            object : CurrentWallpaperInfoFactory.WallpaperInfoCallback {
                override fun onWallpaperInfoCreated(
                    homeWallpaper: WallpaperInfo,
                    lockWallpaper: WallpaperInfo?,
                    presentationMode: Int,
                ) {
                    continuation.resume(Pair(homeWallpaper, lockWallpaper ?: homeWallpaper))
                }
            },
        )
    }
}
