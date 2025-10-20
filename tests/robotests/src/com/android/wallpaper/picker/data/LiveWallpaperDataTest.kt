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

package com.android.wallpaper.picker.data

import android.app.wallpaper.WallpaperDescription
import android.content.Context
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.WallpaperInfoUtils.Companion.createWallpaperInfo
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
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
class LiveWallpaperDataTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun equalityIsAlwaysFalse() {
        val groupName = "group"
        val systemWallpaperInfo = createWallpaperInfo(context)
        val isTitleVisible = false
        val isApplied = true
        val isEffectWallpaper = false
        val effectNames = "effects"
        val contextDescription = "contextDescription"
        val description =
            WallpaperDescription.Builder()
                .setId("id_1")
                .setComponent(systemWallpaperInfo.component)
                .build()
        val data1 =
            LiveWallpaperData(
                groupName,
                systemWallpaperInfo,
                isTitleVisible,
                isApplied,
                isEffectWallpaper,
                effectNames,
                contextDescription,
                description,
            )
        val data2 =
            LiveWallpaperData(
                groupName,
                systemWallpaperInfo,
                isTitleVisible,
                isApplied,
                isEffectWallpaper,
                effectNames,
                contextDescription,
                description,
            )

        assertThat(data1.description).isEqualTo(data2.description)
        assertThat(data1).isNotEqualTo(data2)
    }
}
