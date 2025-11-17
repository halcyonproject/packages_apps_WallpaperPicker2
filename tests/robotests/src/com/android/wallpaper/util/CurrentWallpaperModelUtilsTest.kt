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
import android.net.Uri
import android.os.PersistableBundle
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.data.Destination
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.testing.TestInjector
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

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CurrentWallpaperModelUtilsTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var testInjector: TestInjector

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)
    }

    @Test
    fun createStaticWallpaperModelFromWallpaperDescription() {
        val contextUri = Uri.parse("uri://context")
        val content =
            PersistableBundle().apply {
                putString("picker_metadata_unique_id", "uniqueId")
                putString("picker_metadata_collection_id", "collectionId")
                putInt("picker_metadata_placeholder_color", 250)
                putString("picker_metadata_effects", "someEffect")
            }
        val sourceDescription =
            WallpaperDescription.Builder()
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(contextUri)
                .setContent(content)
                .build()

        val wallpaperModel =
            CurrentWallpaperModelUtils.createStaticWallpaperModelFromDescription(
                context,
                sourceDescription,
                WallpaperManager.FLAG_SYSTEM,
            ) as WallpaperModel.StaticWallpaperModel

        assertThat(wallpaperModel.commonWallpaperData.id.uniqueId).isEqualTo("uniqueId")
        assertThat(wallpaperModel.commonWallpaperData.id.componentName)
            .isEqualTo(ComponentName("StaticWallpaperPackage", "StaticWallpaperClass"))
        assertThat(wallpaperModel.commonWallpaperData.id.collectionId).isEqualTo("collectionId")
        assertThat(wallpaperModel.commonWallpaperData.title).isNull()
        assertThat(wallpaperModel.commonWallpaperData.attributions)
            .isEqualTo(listOf("title", "line1", "line2"))
        assertThat(wallpaperModel.commonWallpaperData.exploreActionUrl).isEqualTo("uri://context")
        assertThat(wallpaperModel.commonWallpaperData.placeholderColorInfo.wallpaperColors).isNull()
        assertThat(wallpaperModel.commonWallpaperData.placeholderColorInfo.placeholderColor)
            .isEqualTo(250)
        assertThat(wallpaperModel.commonWallpaperData.destination)
            .isEqualTo(Destination.APPLIED_TO_SYSTEM)
        assertThat(wallpaperModel.staticWallpaperData.cropHints).isEmpty()
        assertThat(wallpaperModel.downloadableWallpaperData).isNull()
        assertThat(wallpaperModel.networkWallpaperData).isNull()
        assertThat(wallpaperModel.imageWallpaperData).isNull()
    }

    @Test
    fun createStaticWallpaperModelFromWallpaperDescription_applyToLock() {
        val contextUri = Uri.parse("uri://context")
        val content =
            PersistableBundle().apply {
                putString("picker_metadata_unique_id", "uniqueId")
                putString("picker_metadata_collection_id", "collectionId")
                putInt("picker_metadata_placeholder_color", 250)
                putString("picker_metadata_effects", "someEffect")
            }
        val sourceDescription =
            WallpaperDescription.Builder()
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(contextUri)
                .setContent(content)
                .build()

        val wallpaperModel =
            CurrentWallpaperModelUtils.createStaticWallpaperModelFromDescription(
                context,
                sourceDescription,
                WallpaperManager.FLAG_LOCK,
            ) as WallpaperModel.StaticWallpaperModel

        assertThat(wallpaperModel.commonWallpaperData.destination)
            .isEqualTo(Destination.APPLIED_TO_LOCK)
    }
}
