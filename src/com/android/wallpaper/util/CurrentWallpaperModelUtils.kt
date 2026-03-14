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

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.app.WallpaperManager.SetWallpaperFlags
import android.app.wallpaper.WallpaperDescription
import android.app.wallpaper.WallpaperInstance
import android.content.ComponentName
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.VisibleForTesting
import com.android.wallpaper.R
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.asset.BuiltInWallpaperAsset
import com.android.wallpaper.asset.CurrentWallpaperAsset
import com.android.wallpaper.module.CreativeHelper
import com.android.wallpaper.module.WallpaperStatusChecker
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.data.ColorInfo
import com.android.wallpaper.picker.data.CommonWallpaperData
import com.android.wallpaper.picker.data.CreativeWallpaperData
import com.android.wallpaper.picker.data.Destination
import com.android.wallpaper.picker.data.LiveWallpaperData
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

    private const val UNKNOWN_CURRENT_WALLPAPER_ID = "unknown_current_wallpaper_id"
    private const val UNKNOWN_COLLECTION_ID = "unknown_collection_id"

    private const val MULTIPLE_ENGINE_METADATA_NAME: String =
        "com.android.wallpaper.supports_multiple_engines"

    // TODO(b/452460147): Make return type not null and handle nullness
    fun getCurrentWallpaperModels(context: Context): Pair<WallpaperModel, WallpaperModel> {
        var homeWallpaperModel: WallpaperModel
        var lockWallpaperModel: WallpaperModel

        val wallpaperManager = WallpaperManager.getInstance(context)

        val homeWallpaperInstance =
            checkNotNull(wallpaperManager.getWallpaperInstance(WallpaperManager.FLAG_SYSTEM)) {
                "Home wallpaper instance should not be null"
            }
        val lockWallpaperInstance =
            wallpaperManager.getWallpaperInstance(WallpaperManager.FLAG_LOCK)

        val isHomeStatic = (homeWallpaperInstance.info == null)
        val homeDescription = homeWallpaperInstance.description
        if (isHomeStatic) {
            homeWallpaperModel =
                createCurrentStaticWallpaperModelFromDescription(
                    context,
                    homeDescription,
                    WallpaperManager.FLAG_SYSTEM,
                )
        } else {
            homeWallpaperModel =
                createCurrentLiveWallpaperModelFromInstance(
                    context,
                    homeWallpaperInstance,
                    WallpaperManager.FLAG_SYSTEM,
                )
        }

        if (lockWallpaperInstance == null) {
            lockWallpaperModel = homeWallpaperModel
            return Pair(homeWallpaperModel, lockWallpaperModel)
        }

        val isLockStatic = (lockWallpaperInstance.info == null)
        val lockDescription = lockWallpaperInstance.description
        if (isLockStatic) {
            if (isLockWallpaperBuiltIn(wallpaperManager)) {
                lockWallpaperModel =
                    createBuiltInStaticWallpaperModel(context, WallpaperManager.FLAG_LOCK)
            } else {
                lockWallpaperModel =
                    createCurrentStaticWallpaperModelFromDescription(
                        context,
                        lockDescription,
                        WallpaperManager.FLAG_LOCK,
                    )
            }
        } else {
            lockWallpaperModel =
                createCurrentLiveWallpaperModelFromInstance(
                    context,
                    lockWallpaperInstance,
                    WallpaperManager.FLAG_LOCK,
                )
        }
        return Pair(homeWallpaperModel, lockWallpaperModel)
    }

    @VisibleForTesting
    fun createCurrentStaticWallpaperModelFromDescription(
        context: Context,
        wallpaperDescription: WallpaperDescription,
        @SetWallpaperFlags wallpaperManagerDestinationFlag: Int,
    ): WallpaperModel {
        val entryPoint = EntryPoints.get(context, CurrentWallpaperModelUtilsEntryPoint::class.java)

        val uniqueId =
            WallpaperDescriptionUtils.getUniqueId(wallpaperDescription.content)
                ?: UNKNOWN_CURRENT_WALLPAPER_ID + wallpaperManagerDestinationFlag
        val collectionId =
            WallpaperDescriptionUtils.getCollectionId(wallpaperDescription.content)
                ?: UNKNOWN_COLLECTION_ID

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
        val cropHints = getCurrentCropHints(context, wallpaperManagerDestinationFlag)
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
            imageWallpaperData =
                entryPoint
                    .getWallpaperModelConversionHelper()
                    .getImageWallpaperData(wallpaperDescription),
        )
    }

    @VisibleForTesting
    fun createCurrentLiveWallpaperModelFromInstance(
        context: Context,
        wallpaperInstance: WallpaperInstance,
        @SetWallpaperFlags wallpaperManagerDestinationFlag: Int,
    ): WallpaperModel {
        val entryPoint = EntryPoints.get(context, CurrentWallpaperModelUtilsEntryPoint::class.java)

        // WallpaperInfo is not null for Live Wallpaper.
        val wallpaperInfo: WallpaperInfo =
            checkNotNull(wallpaperInstance.info) {
                "WallpaperInfo should not be null for Live Wallpaper"
            }
        val wallpaperDescription: WallpaperDescription = wallpaperInstance.description
        val uniqueId: String = wallpaperInfo.serviceName
        val collectionId: String =
            if (isCreative(context, wallpaperInfo)) {
                WallpaperDescriptionUtils.getCollectionId(wallpaperDescription.content)
                    ?: wallpaperInfo.packageName
            } else {
                WallpaperDescriptionUtils.getCollectionId(wallpaperDescription.content)
                    ?: context.getString(R.string.live_wallpaper_collection_id)
            }

        val wallpaperId: WallpaperId =
            WallpaperId(
                componentName = wallpaperInfo.component,
                uniqueId = uniqueId,
                collectionId = collectionId,
            )
        val destination: Destination =
            if (wallpaperManagerDestinationFlag == WallpaperManager.FLAG_SYSTEM) {
                Destination.APPLIED_TO_SYSTEM
            } else {
                Destination.APPLIED_TO_LOCK
            }
        val destinationForCreativeHelper: WallpaperDestination =
            if (wallpaperManagerDestinationFlag == WallpaperManager.FLAG_SYSTEM) {
                WallpaperDestination.HOME
            } else {
                WallpaperDestination.LOCK
            }
        return WallpaperModel.LiveWallpaperModel(
            commonWallpaperData =
                CommonWallpaperData(
                    id = wallpaperId,
                    title = wallpaperInfo.loadLabel(context.packageManager).toString(),
                    attributions = getLiveWallpaperAttributions(context, wallpaperInfo),
                    exploreActionUrl = getLiveWallpaperActionUri(context, wallpaperInfo),
                    thumbAsset =
                        entryPoint
                            .getWallpaperModelConversionHelper()
                            .getLiveWallpaperThumbAssets(context, wallpaperInfo),
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
            liveWallpaperData =
                LiveWallpaperData(
                    groupName = "",
                    systemWallpaperInfo = wallpaperInfo,
                    isTitleVisible = !isCreative(context, wallpaperInfo),
                    isApplied = true,
                    isEffectWallpaper =
                        ExtendedWallpaperEffectsUtils.isExtendedEffectWallpaper(
                            context,
                            wallpaperInfo.component,
                        ),
                    effectNames =
                        WallpaperDescriptionUtils.getEffects(wallpaperDescription.content),
                    contextDescription = getLiveWallpaperContextDescription(context, wallpaperInfo),
                    description = wallpaperDescription,
                    supportsMultipleEngines =
                        wallpaperInfo.serviceInfo.metaData?.getBoolean(
                            MULTIPLE_ENGINE_METADATA_NAME,
                            false,
                        ) ?: false,
                ),
            creativeWallpaperData =
                if (isCreative(context, wallpaperInfo)) {
                    CreativeWallpaperData(
                        configPreviewUri =
                            entryPoint
                                .getCreativeHelper()
                                .getCreativePreviewUri(
                                    context,
                                    wallpaperInfo,
                                    destinationForCreativeHelper,
                                ),
                        cleanPreviewUri = null,
                        deleteUri = null,
                        thumbnailUri = null,
                        shareUri = null,
                        author = "",
                        description = "",
                        contentDescription = null,
                        isCurrent = true,
                        creativeWallpaperEffectsData = null,
                        isNewCreativeWallpaper = false,
                    )
                } else null,
            internalLiveWallpaperData =
                entryPoint
                    .getWallpaperModelConversionHelper()
                    .getInternalLiveWallpaperData(wallpaperInfo),
        )
    }

    @VisibleForTesting
    fun createBuiltInStaticWallpaperModel(
        context: Context,
        wallpaperManagerDestinationFlag: Int,
    ): WallpaperModel {
        val wallpaperId =
            WallpaperId(
                componentName = ComponentName(STATIC_WALLPAPER_PACKAGE, STATIC_WALLPAPER_CLASS),
                uniqueId = "built-in-wallpaper",
                collectionId = context.getString(R.string.on_device_wallpaper_collection_id),
            )
        val destination =
            if (wallpaperManagerDestinationFlag == WallpaperManager.FLAG_SYSTEM) {
                Destination.APPLIED_TO_SYSTEM
            } else {
                Destination.APPLIED_TO_LOCK
            }
        val asset = BuiltInWallpaperAsset(context)
        return WallpaperModel.StaticWallpaperModel(
            commonWallpaperData =
                CommonWallpaperData(
                    id = wallpaperId,
                    title = null,
                    attributions = emptyList(),
                    exploreActionUrl = null,
                    thumbAsset = asset,
                    placeholderColorInfo =
                        ColorInfo(wallpaperColors = null, placeholderColor = Color.TRANSPARENT),
                    destination = destination,
                ),
            staticWallpaperData =
                StaticWallpaperData(
                    asset = asset,
                    cropHints = getCurrentCropHints(context, wallpaperManagerDestinationFlag),
                ),
            downloadableWallpaperData = null,
            networkWallpaperData = null,
            imageWallpaperData = null,
        )
    }

    private fun isCreative(context: Context, wallpaperInfo: WallpaperInfo): Boolean {
        val entryPoint = EntryPoints.get(context, CurrentWallpaperModelUtilsEntryPoint::class.java)
        return entryPoint.getWallpaperModelConversionHelper().isCreative(wallpaperInfo)
    }

    private fun getLiveWallpaperAttributions(
        context: Context,
        wallpaperInfo: WallpaperInfo,
    ): List<String> {
        val packageManager = context.packageManager
        val attributions = mutableListOf<String>()
        val labelCharSeq = wallpaperInfo.loadLabel(packageManager)
        attributions.add(labelCharSeq.toString())

        try {
            val authorCharSeq = wallpaperInfo.loadAuthor(packageManager)
            if (authorCharSeq != null) {
                attributions.add(authorCharSeq.toString())
            }
        } catch (e: NotFoundException) {
            // No author specified, so no other attribution to add.
        }

        try {
            val descCharSeq = wallpaperInfo.loadDescription(packageManager)
            if (descCharSeq != null) {
                attributions.add(descCharSeq.toString())
            }
        } catch (e: NotFoundException) {
            // No description specified, so no other attribution to add.
        }

        return attributions
    }

    private fun getLiveWallpaperActionUri(context: Context, wallpaperInfo: WallpaperInfo): String? {
        try {
            val wallpaperContextUri = wallpaperInfo.loadContextUri(context.packageManager)
            return wallpaperContextUri.toString() ?: null
        } catch (e: NotFoundException) {
            return null
        }
    }

    private fun getLiveWallpaperContextDescription(
        context: Context,
        wallpaperInfo: WallpaperInfo,
    ): CharSequence? {
        try {
            return wallpaperInfo.loadContextDescription(context.packageManager)
        } catch (e: NotFoundException) {
            return null
        }
    }

    /** Constructs and returns an Asset instance representing the currently-set wallpaper asset. */
    private fun createCurrentWallpaperAsset(
        context: Context,
        flag: Int,
        cropHints: Map<Point, Rect>,
    ): Asset {
        // Whether the wallpaper this object represents is the default built-in wallpaper.
        val entryPoint = EntryPoints.get(context, CurrentWallpaperModelUtilsEntryPoint::class.java)
        val isSystemBuiltIn =
            flag == WallpaperManager.FLAG_SYSTEM &&
                !entryPoint.getWallpaperStatusChecker().isHomeStaticWallpaperSet()
        // Only get the full wallpaper asset when previewing a multi-crop wallpaper, otherwise get
        // the cropped asset.
        val getFullAsset: Boolean = cropHints.isNotEmpty()

        return if (isSystemBuiltIn) BuiltInWallpaperAsset(context)
        else CurrentWallpaperAsset(context, flag, /* getCropped= */ !getFullAsset)
    }

    private fun isLockWallpaperBuiltIn(manager: WallpaperManager): Boolean {
        return manager.lockScreenWallpaperExists() &&
            manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK) == null &&
            manager.getWallpaperFile(WallpaperManager.FLAG_LOCK) == null
    }

    private fun getCurrentCropHints(
        context: Context,
        wallpaperManagerDestinationFlag: Int,
    ): Map<Point, Rect> {
        val entryPoint = EntryPoints.get(context, CurrentWallpaperModelUtilsEntryPoint::class.java)

        val displaySizes =
            entryPoint.getDisplayUtils().getInternalDisplaySizes(allDimensions = true)
        return entryPoint
            .getWallpaperClient()
            .getCurrentCropHints(displaySizes, wallpaperManagerDestinationFlag)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CurrentWallpaperModelUtilsEntryPoint {
        fun getDisplayUtils(): DisplayUtils

        fun getWallpaperClient(): WallpaperClient

        fun getWallpaperStatusChecker(): WallpaperStatusChecker

        fun getWallpaperModelConversionHelper(): WallpaperModelConversionHelper

        fun getCreativeHelper(): CreativeHelper
    }
}
