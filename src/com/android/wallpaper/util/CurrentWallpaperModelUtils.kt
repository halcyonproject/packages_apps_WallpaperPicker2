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

import android.app.WallpaperManager
import android.app.wallpaper.WallpaperDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.VisibleForTesting
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.asset.BuiltInWallpaperAsset
import com.android.wallpaper.asset.CurrentWallpaperAsset
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.data.ColorInfo
import com.android.wallpaper.picker.data.CommonWallpaperData
import com.android.wallpaper.picker.data.Destination
import com.android.wallpaper.picker.data.StaticWallpaperData
import com.android.wallpaper.picker.data.WallpaperId
import com.android.wallpaper.picker.data.WallpaperModel
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Utils for [WallpaperModel].representing current Wallpapaer */
object CurrentWallpaperModelUtils {
    private const val STATIC_WALLPAPER_PACKAGE = "StaticWallpaperPackage"
    private const val STATIC_WALLPAPER_CLASS = "StaticWallpaperClass"

    // TODO(b/452460147): Make return type not null and handle nullness
    fun getCurrentWallpaperModels(context: Context): Pair<WallpaperModel?, WallpaperModel?> {
        var homeWallpaperModel: WallpaperModel?
        var lockWallpaperModel: WallpaperModel?

        val wallpaperManager = WallpaperManager.getInstance(context)

        val homeWallpaperInstance =
            wallpaperManager.getWallpaperInstance(WallpaperManager.FLAG_SYSTEM)

        // TODO(b/452460147): In fact homeWallpaperInstance should not be null
        if (homeWallpaperInstance == null) {
            return Pair(null, null)
        }

        val isHomeStatic = (homeWallpaperInstance.info == null)
        val homeDescription = homeWallpaperInstance.description
        if (isHomeStatic) {
            homeWallpaperModel =
                createStaticWallpaperModelFromDescription(
                    context,
                    homeDescription,
                    WallpaperManager.FLAG_SYSTEM,
                )
        } else {
            // TODO(b/452460147): Handle LiveWallpaper
            homeWallpaperModel = null
        }
        // TODO(b/452460147): Handle lock wallpaper (static and Live)
        lockWallpaperModel = homeWallpaperModel
        return Pair(homeWallpaperModel, lockWallpaperModel)
    }

    @VisibleForTesting
    fun createStaticWallpaperModelFromDescription(
        context: Context,
        wallpaperDescription: WallpaperDescription,
        wallpaperManagerDestinationFlag: Int,
    ): WallpaperModel {
        val entryPoint = EntryPoints.get(context, CurrentWallpaperModelUtilsEntryPoint::class.java)
        val displayUtils = entryPoint.getDisplayUtils()
        val wallpaperClient = entryPoint.getWallpaperClient()

        val uniqueId = WallpaperDescriptionUtils.getUniqueId(wallpaperDescription.content) ?: ""
        val collectionId =
            WallpaperDescriptionUtils.getCollectionId(wallpaperDescription.content) ?: ""

        val displaySizes = displayUtils.getInternalDisplaySizes(allDimensions = true)
        val cropHints =
            wallpaperClient.getCurrentCropHints(displaySizes, wallpaperManagerDestinationFlag)
        val wallpaperId =
            WallpaperId(
                componentName = ComponentName(STATIC_WALLPAPER_PACKAGE, STATIC_WALLPAPER_CLASS),
                uniqueId = uniqueId,
                collectionId = collectionId,
            )
        val destination =
            if (wallpaperManagerDestinationFlag == WallpaperManager.FLAG_SYSTEM) {
                Destination.APPLIED_TO_SYSTEM
            } else {
                Destination.APPLIED_TO_LOCK
            }
        return WallpaperModel.StaticWallpaperModel(
            commonWallpaperData =
                CommonWallpaperData(
                    id = wallpaperId,
                    title = null,
                    attributions =
                        listOf(wallpaperDescription.title.toString()) +
                            wallpaperDescription.description.map { description ->
                                description.toString()
                            },
                    exploreActionUrl = wallpaperDescription.contextUri.toString(),
                    thumbAsset =
                        createCurrentWallpaperAsset(
                            context,
                            wallpaperManagerDestinationFlag,
                            cropHints,
                        ),
                    placeholderColorInfo =
                        ColorInfo(
                            wallpaperColors = null,
                            placeholderColor =
                                WallpaperDescriptionUtils.getPlaceHolderColor(
                                    wallpaperDescription.content
                                ) ?: Color.TRANSPARENT,
                        ),
                    destination = destination,
                ),
            staticWallpaperData =
                StaticWallpaperData(
                    asset =
                        createCurrentWallpaperAsset(
                            context,
                            wallpaperManagerDestinationFlag,
                            cropHints,
                        ),
                    cropHints = cropHints,
                ),
            downloadableWallpaperData = null,
            networkWallpaperData = null,
            imageWallpaperData = null,
        )
    }

    /** Constructs and returns an Asset instance representing the currently-set wallpaper asset. */
    private fun createCurrentWallpaperAsset(
        context: Context,
        flag: Int,
        cropHints: Map<Point, Rect>,
    ): Asset {
        // Whether the wallpaper this object represents is the default built-in wallpaper.
        // TODO(b/452460147): Remove the usage of Injector here
        val isSystemBuiltIn =
            flag == WallpaperManager.FLAG_SYSTEM &&
                !InjectorProvider.getInjector()
                    .getWallpaperStatusChecker(context)
                    .isHomeStaticWallpaperSet()
        // Only get the full wallpaper asset when previewing a multi-crop wallpaper, otherwise get
        // the cropped asset.
        val getFullAsset: Boolean = cropHints.isNotEmpty()

        return if (isSystemBuiltIn) BuiltInWallpaperAsset(context)
        else CurrentWallpaperAsset(context, flag, /* getCropped= */ !getFullAsset)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CurrentWallpaperModelUtilsEntryPoint {
        fun getDisplayUtils(): DisplayUtils

        fun getWallpaperClient(): WallpaperClient
    }
}
