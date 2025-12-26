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

import android.app.WallpaperInfo
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import org.robolectric.Shadows.shadowOf

/** Utility methods for writing Robolectric tests using [android.app.WallpaperInfo]. */
class WallpaperInfoUtils {
    companion object {
        const val STUB_PACKAGE = "com.google.android.apps.wallpaper.nexus"
        const val WALLPAPER_CLASS = "NewYorkWallpaper"
        const val WALLPAPER_SPLIT = "wallpaper_cities_ny"

        /**
         * Creates an instance of [android.app.WallpaperInfo] This method must be called from a test
         * that uses [com.android.wallpaper.testing.ShadowWallpaperInfo].
         */
        fun createWallpaperInfo(
            context: Context,
            stubPackage: String,
            wallpaperClass: String,
        ): WallpaperInfo {
            return createWallpaperInfo(
                context = context,
                componentName = ComponentName(stubPackage, wallpaperClass),
            )
        }

        /**
         * Creates an instance of [android.app.WallpaperInfo]
         *
         * <p>Set [configureService] to register the associated service so that it will resolve if
         * necessary.
         *
         * <p>Add any additional actions to [extraActions] so that queryIntentServices will return
         * this wallpaper for that action. This ensures that PackageManager#resolveService in
         * RecentWallpaperUtils#cleanUpRecentsArray returns non-null so that our test entry isn't
         * removed from the recents list.
         *
         * <p>Set [configureCreative] to specify that this wallpaper should be returned when
         * querying creative wallpapers. For example, including
         * CreativeCategoryFetcher.CREATIVE_ACTION will add this wallpaper to those returned when
         * querying creative wallpapers.
         *
         * <p>This method must be called from a test that uses
         * [com.android.wallpaper.testing.ShadowWallpaperInfo].
         */
        fun createWallpaperInfo(
            context: Context,
            componentName: ComponentName = ComponentName(STUB_PACKAGE, WALLPAPER_CLASS),
            wallpaperSplit: String = WALLPAPER_SPLIT,
            configureService: Boolean = true,
            extraActions: List<String> = listOf(),
            metaData: Bundle? = null,
        ): WallpaperInfo {
            val resolveInfo =
                ResolveInfo().apply {
                    serviceInfo = ServiceInfo()
                    serviceInfo.packageName = componentName.packageName
                    serviceInfo.name = componentName.className
                    serviceInfo.splitName = wallpaperSplit
                    serviceInfo.flags = PackageManager.GET_META_DATA
                    serviceInfo.metaData = metaData
                    nonLocalizedLabel = "nonLocalizedLabel"
                }
            // ShadowWallpaperInfo allows the creation of this object
            val wallpaperInfo = WallpaperInfo(context, resolveInfo)
            val actions =
                extraActions.toMutableList().apply {
                    if (configureService) add(WallpaperService.SERVICE_INTERFACE)
                }
            if (actions.isNotEmpty()) {
                val pm = shadowOf(context.packageManager)
                pm.addOrUpdateService(resolveInfo.serviceInfo)
                actions.forEach { action ->
                    pm.addIntentFilterForService(componentName, IntentFilter(action))
                }
                // For live wallpapers, we need the call to PackageManager#resolveService in
                // RecentWallpaperUtils#cleanUpRecentsArray to return non-null so that our test
                // entry isn't removed from the recents list.
            }
            return wallpaperInfo
        }
    }
}
