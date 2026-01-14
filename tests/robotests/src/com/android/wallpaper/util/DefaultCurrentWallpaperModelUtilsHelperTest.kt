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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.asset.LiveWallpaperThumbAsset
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.WallpaperInfoUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class DefaultCurrentWallpaperModelUtilsHelperTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var currentWallpaperModelUtilsHelper: DefaultCurrentWallpaperModelUtilsHelper

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)

        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun getLiveWallpaperThumbAssets_returnsAsset() {
        val wallpaperInfo = WallpaperInfoUtils.createWallpaperInfo(context)

        val asset =
            currentWallpaperModelUtilsHelper.getLiveWallpaperThumbAssets(context, wallpaperInfo)

        assertThat(asset).isNotNull()
        assertThat(asset).isInstanceOf(LiveWallpaperThumbAsset::class.java)
    }
}
