/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.module

import android.app.WallpaperManager
import android.content.Context
import com.android.wallpaper.model.CreativeWallpaperInfo
import com.android.wallpaper.model.CurrentWallpaperInfo
import com.android.wallpaper.model.DefaultWallpaperInfo
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.LiveWallpaperMetadata
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.WallpaperMetadata
import com.android.wallpaper.module.WallpaperPreferences.PresentationMode

/**
 * Default implementation of [CurrentWallpaperInfoFactory] which actually constructs [WallpaperInfo]
 * instances representing the wallpapers currently set to the device.
 */
class DefaultCurrentWallpaperInfoFactory(
    private val mWallpaperRefresher: WallpaperRefresher,
    private val mLiveWallpaperInfoFactory: LiveWallpaperInfoFactory,
) : CurrentWallpaperInfoFactory {
    // Cached copies of the currently-set WallpaperInfo(s) and presentation mode.
    private var mHomeWallpaper: WallpaperInfo? = null
    private var mLockWallpaper: WallpaperInfo? = null

    @PresentationMode private var mPresentationMode = 0

    @Synchronized
    override fun createCurrentWallpaperInfos(
        context: Context,
        forceRefresh: Boolean,
        callback: CurrentWallpaperInfoFactory.WallpaperInfoCallback,
    ) {
        val isHomeWallpaperSynced = homeWallpaperSynced(context)
        val isLockWallpaperSynced = lockWallpaperSynced(context)
        if (
            !forceRefresh &&
                isHomeWallpaperSynced &&
                isLockWallpaperSynced &&
                mPresentationMode != WallpaperPreferences.PRESENTATION_MODE_ROTATING
        ) {
            // Update wallpaper crop hints for static wallpaper even if home & lock wallpaper are
            // considered synced because wallpaper info are considered synced as long as both are
            // static
            val displayUtils = InjectorProvider.getInjector().getDisplayUtils(context)
            val wallpaperClient = InjectorProvider.getInjector().getWallpaperClient(context)
            val displaySizes = displayUtils.getInternalDisplaySizes(/* allDimensions= */ true)
            if (mHomeWallpaper != null) {
                val isHomeWallpaperStatic =
                    mHomeWallpaper!!.wallpaperComponent == null ||
                        mHomeWallpaper!!.wallpaperComponent.component == null
                if (isHomeWallpaperStatic) {
                    mHomeWallpaper!!.wallpaperCropHints =
                        wallpaperClient.getCurrentCropHints(
                            displaySizes,
                            WallpaperManager.FLAG_SYSTEM,
                        )
                } else {
                    mHomeWallpaper!!.wallpaperCropHints = HashMap()
                }
            }
            if (mLockWallpaper != null) {
                val isLockWallpaperStatic =
                    mLockWallpaper!!.wallpaperComponent == null ||
                        mLockWallpaper!!.wallpaperComponent.component == null
                if (isLockWallpaperStatic) {
                    mLockWallpaper!!.wallpaperCropHints =
                        wallpaperClient.getCurrentCropHints(
                            displaySizes,
                            WallpaperManager.FLAG_LOCK,
                        )
                } else {
                    mLockWallpaper!!.wallpaperCropHints = HashMap()
                }
            }
            callback.onWallpaperInfoCreated(mHomeWallpaper!!, mLockWallpaper, mPresentationMode)
            return
        }

        // Clear cached copies if we are refreshing the currently-set WallpaperInfo(s) from the
        // Refresher so that multiple calls to this method after a call with forceRefresh=true don't
        // provide old cached copies.
        if (forceRefresh) {
            clearCurrentWallpaperInfos()
        }

        mWallpaperRefresher.refresh {
            homeWallpaperMetadata: WallpaperMetadata,
            lockWallpaperMetadata: WallpaperMetadata?,
            presentationMode: Int ->
            val homeWallpaper: WallpaperInfo
            if (homeWallpaperMetadata is LiveWallpaperMetadata) {
                homeWallpaper =
                    mLiveWallpaperInfoFactory.getLiveWallpaperInfo(
                        homeWallpaperMetadata.getWallpaperComponent()
                    )
                (homeWallpaper as LiveWallpaperInfo).wallpaperDescription =
                    homeWallpaperMetadata.description
                updateIfCreative(homeWallpaper, homeWallpaperMetadata)
            } else {
                val imageUri = homeWallpaperMetadata.wallpaperImageUri
                homeWallpaper =
                    CurrentWallpaperInfo(
                        homeWallpaperMetadata.attributions,
                        homeWallpaperMetadata.actionUrl,
                        homeWallpaperMetadata.collectionId,
                        WallpaperManager.FLAG_SYSTEM,
                        imageUri,
                        homeWallpaperMetadata.id,
                    )
                homeWallpaper.setWallpaperCropHints(homeWallpaperMetadata.wallpaperCropHints)
            }

            var lockWallpaper: WallpaperInfo? = null

            if (lockWallpaperMetadata != null) {
                if (lockWallpaperMetadata is LiveWallpaperMetadata) {
                    lockWallpaper =
                        mLiveWallpaperInfoFactory.getLiveWallpaperInfo(
                            lockWallpaperMetadata.getWallpaperComponent()
                        )
                    (lockWallpaper as LiveWallpaperInfo).wallpaperDescription =
                        lockWallpaperMetadata.description
                    updateIfCreative(lockWallpaper, lockWallpaperMetadata)
                } else {
                    if (isLockWallpaperBuiltIn(context)) {
                        lockWallpaper = DefaultWallpaperInfo()
                    } else {
                        val imageUri = lockWallpaperMetadata.wallpaperImageUri
                        lockWallpaper =
                            CurrentWallpaperInfo(
                                lockWallpaperMetadata.attributions,
                                lockWallpaperMetadata.actionUrl,
                                lockWallpaperMetadata.collectionId,
                                WallpaperManager.FLAG_LOCK,
                                imageUri,
                                lockWallpaperMetadata.id,
                            )
                    }

                    lockWallpaper.wallpaperCropHints = lockWallpaperMetadata.wallpaperCropHints
                }
            }

            mHomeWallpaper = homeWallpaper
            mLockWallpaper = lockWallpaper
            mPresentationMode = presentationMode
            callback.onWallpaperInfoCreated(homeWallpaper, lockWallpaper, presentationMode)
        }
    }

    private fun updateIfCreative(info: WallpaperInfo, metadata: WallpaperMetadata) {
        if ((info is CreativeWallpaperInfo) && (metadata is LiveWallpaperMetadata)) {
            info.configPreviewUri = metadata.previewUri
        }
    }

    private fun isLockWallpaperBuiltIn(context: Context): Boolean {
        val manager = context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager

        return manager.lockScreenWallpaperExists() &&
            manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK) == null &&
            manager.getWallpaperFile(WallpaperManager.FLAG_LOCK) == null
    }

    /**
     * We check 2 things in this function:
     * 1. If mHomeWallpaper is null, the wallpaper is not initialized. Return false.
     * 2. In the case when mHomeWallpaper is not null, we check if mHomeWallpaper is synced with the
     *    one from the wallpaper manager.
     */
    private fun homeWallpaperSynced(context: Context): Boolean {
        if (mHomeWallpaper == null) {
            return false
        }
        return wallpaperSynced(context, mHomeWallpaper, WallpaperManager.FLAG_SYSTEM)
    }

    /**
     * mLockWallpaper can be null even after initialization. We only check the case if the
     * lockscreen wallpaper is synced.
     */
    private fun lockWallpaperSynced(context: Context): Boolean {
        return wallpaperSynced(context, mLockWallpaper, WallpaperManager.FLAG_LOCK)
    }

    /**
     * Check if the given wallpaper info is synced with the one from the wallpaper manager. We only
     * try to get the underlying ComponentName from both sides. If both are null, it means both are
     * static image wallpapers, or both are not set, which we consider synced and return true. If
     * only of the them is null, it means one is static image wallpaper and another is live
     * wallpaper. We should return false. If both are not null, we check if the two ComponentName(s)
     * are equal.
     */
    private fun wallpaperSynced(
        context: Context,
        wallpaperInfo: WallpaperInfo?,
        which: Int,
    ): Boolean {
        val currentWallpaperInfo = WallpaperManager.getInstance(context).getWallpaperInfo(which)
        val currentComponentName = currentWallpaperInfo?.component
        val info = wallpaperInfo?.wallpaperComponent
        val homeComponentName = info?.component
        return if (currentComponentName == null) {
            // If both are null, it might not be synced for LOCK (param which is 2):
            // When previous LOCK is default static then homeComponentName will be null, and current
            // wallpaper is live for both home and lock then currentComponentName will be null.
            if (homeComponentName == null) {
                which != WallpaperManager.FLAG_LOCK
            } else {
                false
            }
        } else if (homeComponentName == null) {
            // currentComponentName not null and homeComponentName null. It's not synced.
            false
        } else {
            currentComponentName == homeComponentName
        }
    }

    override fun clearCurrentWallpaperInfos() {
        mHomeWallpaper = null
        mLockWallpaper = null
    }
}
