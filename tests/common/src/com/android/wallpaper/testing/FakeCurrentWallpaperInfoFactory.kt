/*
 * Copyright (C) 2019 The Android Open Source Project
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
import com.android.wallpaper.model.WallpaperMetadata
import com.android.wallpaper.module.CurrentWallpaperInfoFactory
import com.android.wallpaper.module.WallpaperRefresher
import javax.inject.Inject
import javax.inject.Singleton

/** Test double of [CurrentWallpaperInfoFactory]. */
@Singleton
class FakeCurrentWallpaperInfoFactory @Inject constructor(refresher: FakeWallpaperRefresher) :
    CurrentWallpaperInfoFactory {
    private val mRefresher: WallpaperRefresher = refresher
    private var homeWallpaper: WallpaperInfo? = null
    private var lockWallpaper: WallpaperInfo? = null

    /**
     * Uses [mHomeWallpaper] and [mLockWallpaper] if provided, otherwise creates instance(s) of
     * [TestStaticWallpaperInfo] from the data given by [WallpaperRefresher].
     */
    override fun createCurrentWallpaperInfos(
        context: Context,
        forceRefresh: Boolean,
        callback: CurrentWallpaperInfoFactory.WallpaperInfoCallback,
    ) {
        mRefresher.refresh {
            homeWallpaperMetadata: WallpaperMetadata,
            lockWallpaperMetadata: WallpaperMetadata?,
            presentationMode: Int ->
            val homeWallpaper =
                if ((homeWallpaper != null)) homeWallpaper!!
                else
                    createTestWallpaperInfo(
                        homeWallpaperMetadata.attributions,
                        homeWallpaperMetadata.actionUrl,
                        homeWallpaperMetadata.collectionId,
                    )
            var lockWallpaper = lockWallpaper
            if (lockWallpaper == null && lockWallpaperMetadata != null) {
                lockWallpaper =
                    createTestWallpaperInfo(
                        lockWallpaperMetadata.attributions,
                        lockWallpaperMetadata.actionUrl,
                        lockWallpaperMetadata.collectionId,
                    )
            }
            callback.onWallpaperInfoCreated(homeWallpaper, lockWallpaper, presentationMode)
        }
    }

    override fun clearCurrentWallpaperInfos() {}

    fun setHomeWallpaper(info: WallpaperInfo?) {
        homeWallpaper = info
    }

    fun setLockWallpaper(info: WallpaperInfo?) {
        lockWallpaper = info
    }

    companion object {
        private fun createTestWallpaperInfo(
            attributions: List<String>,
            actionUrl: String,
            collectionId: String,
        ): WallpaperInfo {
            val wallpaper = TestStaticWallpaperInfo(TestStaticWallpaperInfo.COLOR_DEFAULT)
            wallpaper.setAttributions(attributions)
            wallpaper.setActionUrl(actionUrl)
            wallpaper.setCollectionId(collectionId)
            return wallpaper
        }
    }
}
