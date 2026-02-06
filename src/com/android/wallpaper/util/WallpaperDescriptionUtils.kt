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
import android.app.wallpaper.WallpaperDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.os.PersistableBundle
import android.service.wallpaper.WallpaperService
import android.text.TextUtils
import android.util.Log
import androidx.core.net.toUri
import com.android.wallpaper.asset.LiveWallpaperThumbAsset
import com.android.wallpaper.picker.data.ColorInfo
import com.android.wallpaper.picker.data.CommonWallpaperData
import com.android.wallpaper.picker.data.Destination
import com.android.wallpaper.picker.data.LiveWallpaperData
import com.android.wallpaper.picker.data.WallpaperId
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.updateMetadata

/**
 * Utilities for [WallpaperDescription], such as manipulating picker-specific metadata in the
 * content bundle.
 */
class WallpaperDescriptionUtils {

    companion object {
        const val TAG = "WallpaperDescriptionUtils"

        const val MULTIPLE_ENGINE_METADATA_NAME: String =
            "com.android.wallpaper.supports_multiple_engines"

        private const val CONTENT_KEY_COLLECTION_ID = "picker_metadata_collection_id"
        private const val CONTENT_KEY_PLACEHOLDER_COLOR = "picker_metadata_placeholder_color"
        private const val CONTENT_KEY_UNIQUE_ID = "picker_metadata_unique_id"
        private const val CONTENT_KEY_EFFECTS = "picker_metadata_effects"

        /**
         * Updates the content bundle with picker-specific metadata.
         *
         * Changes the bundle in place, and also returns the updated bundle for convenience.
         */
        fun updateMetadata(
            content: PersistableBundle,
            collectionId: String?,
            placeHolderColor: Int?,
            uniqueId: String?,
            effects: String?,
        ): PersistableBundle {
            return content.apply {
                putString(CONTENT_KEY_COLLECTION_ID, collectionId)
                putString(CONTENT_KEY_UNIQUE_ID, uniqueId)
                placeHolderColor?.let { putInt(CONTENT_KEY_PLACEHOLDER_COLOR, it) }
                effects?.let { putString(CONTENT_KEY_EFFECTS, it) }
            }
        }

        fun getCollectionId(content: PersistableBundle): String? {
            return content.getString(CONTENT_KEY_COLLECTION_ID)
        }

        fun getUniqueId(content: PersistableBundle): String? {
            return content.getString(CONTENT_KEY_UNIQUE_ID)
        }

        fun getPlaceHolderColor(content: PersistableBundle): Int? {
            return if (content.containsKey(CONTENT_KEY_PLACEHOLDER_COLOR))
                content.getInt(CONTENT_KEY_PLACEHOLDER_COLOR)
            else null
        }

        fun getEffects(content: PersistableBundle): String? {
            return content.getString(CONTENT_KEY_EFFECTS)
        }

        fun createWallpaperInfoFromDescription(
            context: Context,
            description: WallpaperDescription,
        ): WallpaperInfo {
            val componentName =
                description.component
                    ?: throw IllegalArgumentException(
                        "Must be valid live wallpaper, component name is null"
                    )
            var packageName: String = componentName.packageName
            var serviceName: String = componentName.className
            if (TextUtils.isEmpty(packageName)) {
                val parts: List<String> = serviceName.split("/")
                if (parts != null && parts.size == 2) {
                    packageName = parts[0]
                    serviceName = parts[1]
                } else {
                    throw IllegalArgumentException(
                        "Must be valid live wallpaper, service name is invalid"
                    )
                }
            }
            val intent: Intent = Intent(WallpaperService.SERVICE_INTERFACE)
            intent.setClassName(packageName!!, serviceName)
            val resolveInfos: List<ResolveInfo> =
                context.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
            if (resolveInfos.isEmpty()) {
                throw IllegalArgumentException("Must be valid live wallpaper, service is not found")
            }
            try {
                return WallpaperInfo(context, resolveInfos.get(0))
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Must be valid live wallpaper, failed to create WallpaperInfo"
                )
            }
        }
    }
}

fun LiveWallpaperModel.toDescription(): WallpaperDescription {
    val content =
        updateMetadata(
            liveWallpaperData.description.content,
            commonWallpaperData.id.collectionId,
            commonWallpaperData.placeholderColorInfo.placeholderColor,
            commonWallpaperData.id.uniqueId,
            liveWallpaperData.effectNames,
        )
    val attribs = commonWallpaperData.attributions
    val title = attribs?.getOrNull(0)
    val desc = attribs?.slice(1 until attribs.size).orEmpty()
    return liveWallpaperData.description.let { source ->
        source
            .toBuilder()
            .setTitle(source.title ?: title)
            .setDescription(if (source.description.size > 0) source.description else desc)
            .setContextUri(source.contextUri ?: commonWallpaperData.exploreActionUrl?.toUri())
            .setContent(content)
            .build()
    }
}

fun StaticWallpaperModel.toDescription(
    id: String,
    cropHints: Map<Point, Rect>,
): WallpaperDescription {
    val content =
        updateMetadata(
            PersistableBundle(),
            commonWallpaperData.id.collectionId,
            commonWallpaperData.placeholderColorInfo.placeholderColor,
            commonWallpaperData.id.uniqueId,
            null,
        )
    val attribs = commonWallpaperData.attributions
    val title = attribs?.getOrNull(0)
    val desc = attribs?.slice(1 until attribs.size).orEmpty()
    return WallpaperDescription.Builder()
        .setId(id)
        .setTitle(title)
        .setDescription(desc)
        .setContextUri(commonWallpaperData.exploreActionUrl?.toUri())
        .setCropHints(cropHints)
        .setContent(content)
        .build()
}

// This function can only be used to convert a WallpaperDescription to LiveWallpaperModel for
// previewing purposes, because it contains logic specific to previewing wallpaper with effects. Do
// not use it elsewhere.
fun WallpaperDescription.toLiveWallpaperModel(context: Context): LiveWallpaperModel? {
    if (this.component == null) {
        Log.e(
            WallpaperDescriptionUtils.TAG,
            "WallpaperDescription.toLiveWallpaperModel: component is null",
        )
        return null
    }
    val wallpaperInfo: WallpaperInfo
    try {
        wallpaperInfo = WallpaperDescriptionUtils.createWallpaperInfoFromDescription(context, this)
    } catch (e: Exception) {
        Log.e(
            WallpaperDescriptionUtils.TAG,
            "WallpaperDescription.toLiveWallpaperModel: failed to create WallpaperInfo",
        )
        return null
    }

    val uniqueId: String = WallpaperDescriptionUtils.getUniqueId(this.content) ?: ""

    val wallpaperId: WallpaperId =
        WallpaperId(
            componentName = this.component!!,
            uniqueId =
                if (this.id != null) "${this.component!!.className}_${this.id}"
                else this.component!!.className,
            // CollectionId is not recoverable from the WallpaperDescription.
            // To keep logging works, we hardcode "image_wallpapers" as collection Id.
            collectionId = "image_wallpapers",
        )
    val destination: Destination = Destination.NOT_APPLIED
    return WallpaperModel.LiveWallpaperModel(
        commonWallpaperData =
            CommonWallpaperData(
                id = wallpaperId,
                title = this.title.toString(),
                // Attributions is not recoverable from the WallpaperDescription.
                attributions =
                    listOf(this.title.toString()) +
                        this.description.map { description -> description.toString() },
                exploreActionUrl = this.contextUri?.toString(),
                thumbAsset = LiveWallpaperThumbAsset(context, wallpaperInfo),
                placeholderColorInfo =
                    ColorInfo(
                        wallpaperColors = null,
                        placeholderColor =
                            WallpaperDescriptionUtils.getPlaceHolderColor(this.content)
                                ?: Color.TRANSPARENT,
                    ),
                destination = destination,
            ),
        liveWallpaperData =
            LiveWallpaperData(
                groupName = "",
                systemWallpaperInfo = wallpaperInfo,
                isTitleVisible = false,
                isApplied = false,
                isEffectWallpaper =
                    ExtendedWallpaperEffectsUtils.isExtendedEffectWallpaper(
                        context,
                        this.component!!,
                    ),
                // WallpaperDescriptionUtils.getEffects() does not work for thie field as the key is
                // different.
                effectNames = this.content.getString("EffectName") ?: "",
                contextDescription = this.contextDescription,
                description = this,
                supportsMultipleEngines =
                    wallpaperInfo.serviceInfo.metaData?.getBoolean(
                        WallpaperDescriptionUtils.MULTIPLE_ENGINE_METADATA_NAME,
                        false,
                    ) ?: false,
            ),
        // For preview purposes, the creative wallpaper data is not needed for now.
        creativeWallpaperData = null,
        internalLiveWallpaperData = null,
    )
}
