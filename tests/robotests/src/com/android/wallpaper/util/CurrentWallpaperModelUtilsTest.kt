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

import android.app.Application
import android.app.WallpaperManager
import android.app.wallpaper.WallpaperDescription
import android.app.wallpaper.WallpaperInstance
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.PersistableBundle
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.asset.BuiltInWallpaperAsset
import com.android.wallpaper.asset.CurrentWallpaperAsset
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.data.Destination
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestAsset
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.WallpaperInfoUtils
import com.android.wallpaper.testing.TestWallpaperStatusChecker
import com.android.wallpaper.module.WallpaperStatusChecker
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class CurrentWallpaperModelUtilsTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var wallpaperStatusChecker: WallpaperStatusChecker

    @Mock lateinit var wallpaperManager: WallpaperManager

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)

        context = ApplicationProvider.getApplicationContext()
        val shadowApplication = Shadows.shadowOf(context as Application)
        shadowApplication.setSystemService(Context.WALLPAPER_SERVICE, wallpaperManager)
    }

    @Test
    fun getCurrentWallpaperModels_homeAndLockStaticSame() {
        val homeDescription: WallpaperDescription =
            WallpaperDescription.Builder()
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(Uri.parse("uri://context"))
                .setContent(
                    PersistableBundle().apply {
                        putString("picker_metadata_unique_id", "uniqueId")
                        putString("picker_metadata_collection_id", "collectionId")
                        putInt("picker_metadata_placeholder_color", 250)
                        putString("picker_metadata_effects", "someEffect")
                    }
                )
                .build()
        val homeInstance = WallpaperInstance(null, homeDescription, null)
        `when`(wallpaperManager.getWallpaperInstance(WallpaperManager.FLAG_SYSTEM))
            .thenReturn(homeInstance)
        `when`(wallpaperManager.getWallpaperInstance(WallpaperManager.FLAG_LOCK)).thenReturn(null)

        val wallpaperModels = CurrentWallpaperModelUtils.getCurrentWallpaperModels(context)

        assertThat(wallpaperModels.first).isNotNull()
        assertThat(wallpaperModels.second).isNotNull()
        assertThat(wallpaperModels.first).isEqualTo(wallpaperModels.second)
    }

    @Test
    fun getCurrentWallpaperModels_homeAndLockStaticDifferent() {
        val homeDescription: WallpaperDescription =
            WallpaperDescription.Builder()
                .setId("id1")
                .setTitle("title1")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(Uri.parse("uri://context"))
                .setContent(
                    PersistableBundle().apply {
                        putString("picker_metadata_unique_id", "uniqueId1")
                        putString("picker_metadata_collection_id", "collectionId")
                        putInt("picker_metadata_placeholder_color", 250)
                        putString("picker_metadata_effects", "someEffect")
                    }
                )
                .build()
        val lockDescription: WallpaperDescription =
            WallpaperDescription.Builder()
                .setId("id2")
                .setTitle("title2")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(Uri.parse("uri://context"))
                .setContent(
                    PersistableBundle().apply {
                        putString("picker_metadata_unique_id", "uniqueId2")
                        putString("picker_metadata_collection_id", "collectionId")
                        putInt("picker_metadata_placeholder_color", 250)
                        putString("picker_metadata_effects", "someEffect")
                    }
                )
                .build()
        val homeInstance = WallpaperInstance(null, homeDescription, null)
        val lockInstance = WallpaperInstance(null, lockDescription, null)
        `when`(wallpaperManager.getWallpaperInstance(WallpaperManager.FLAG_SYSTEM))
            .thenReturn(homeInstance)
        `when`(wallpaperManager.getWallpaperInstance(WallpaperManager.FLAG_LOCK))
            .thenReturn(lockInstance)

        val wallpaperModels = CurrentWallpaperModelUtils.getCurrentWallpaperModels(context)

        assertThat(wallpaperModels.first).isNotNull()
        assertThat(wallpaperModels.second).isNotNull()
        assertThat(wallpaperModels.first).isNotEqualTo(wallpaperModels.second)
    }

    @Test
    fun createStaticWallpaperModelFromWallpaperDescription() {
        val sourceDescription =
            WallpaperDescription.Builder()
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(Uri.parse("uri://context"))
                .setContent(
                    PersistableBundle().apply {
                        putString("picker_metadata_unique_id", "uniqueId")
                        putString("picker_metadata_collection_id", "collectionId")
                        putInt("picker_metadata_placeholder_color", 250)
                        putString("picker_metadata_effects", "someEffect")
                    }
                )
                .build()

        val wallpaperModel =
            CurrentWallpaperModelUtils.createCurrentStaticWallpaperModelFromDescription(
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
        assertThat(wallpaperModel.commonWallpaperData.thumbAsset)
            .isInstanceOf(CurrentWallpaperAsset::class.java)
        assertThat(wallpaperModel.commonWallpaperData.thumbAsset)
            .isNotInstanceOf(BuiltInWallpaperAsset::class.java)
        assertThat(wallpaperModel.staticWallpaperData.asset)
            .isInstanceOf(CurrentWallpaperAsset::class.java)
        assertThat(wallpaperModel.staticWallpaperData.asset)
            .isNotInstanceOf(BuiltInWallpaperAsset::class.java)
        assertThat(wallpaperModel.staticWallpaperData.cropHints).isEmpty()
        assertThat(wallpaperModel.downloadableWallpaperData).isNull()
        assertThat(wallpaperModel.networkWallpaperData).isNull()
        assertThat(wallpaperModel.imageWallpaperData).isNull()
    }

    @Test
    fun createStaticWallpaperModelFromWallpaperDescription_builtInAsset() {
        (wallpaperStatusChecker as TestWallpaperStatusChecker).setHomeStaticWallpaperSet(false)
        val sourceDescription =
            WallpaperDescription.Builder()
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(Uri.parse("uri://context"))
                .setContent(
                    PersistableBundle().apply {
                        putString("picker_metadata_unique_id", "uniqueId")
                        putString("picker_metadata_collection_id", "collectionId")
                        putInt("picker_metadata_placeholder_color", 250)
                        putString("picker_metadata_effects", "someEffect")
                    }
                )
                .build()

        val wallpaperModel =
            CurrentWallpaperModelUtils.createCurrentStaticWallpaperModelFromDescription(
                context,
                sourceDescription,
                WallpaperManager.FLAG_SYSTEM,
            ) as WallpaperModel.StaticWallpaperModel

        assertThat(wallpaperModel.commonWallpaperData.thumbAsset)
            .isInstanceOf(BuiltInWallpaperAsset::class.java)
        assertThat(wallpaperModel.commonWallpaperData.thumbAsset)
            .isNotInstanceOf(CurrentWallpaperAsset::class.java)
        assertThat(wallpaperModel.staticWallpaperData.asset)
            .isInstanceOf(BuiltInWallpaperAsset::class.java)
        assertThat(wallpaperModel.staticWallpaperData.asset)
            .isNotInstanceOf(CurrentWallpaperAsset::class.java)
    }

    @Test
    fun createStaticWallpaperModelFromWallpaperDescription_applyToLock() {
        val sourceDescription =
            WallpaperDescription.Builder()
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(Uri.parse("uri://context"))
                .setContent(
                    PersistableBundle().apply {
                        putString("picker_metadata_unique_id", "uniqueId")
                        putString("picker_metadata_collection_id", "collectionId")
                        putInt("picker_metadata_placeholder_color", 250)
                        putString("picker_metadata_effects", "someEffect")
                    }
                )
                .build()

        val wallpaperModel =
            CurrentWallpaperModelUtils.createCurrentStaticWallpaperModelFromDescription(
                context,
                sourceDescription,
                WallpaperManager.FLAG_LOCK,
            ) as WallpaperModel.StaticWallpaperModel

        assertThat(wallpaperModel.commonWallpaperData.destination)
            .isEqualTo(Destination.APPLIED_TO_LOCK)
    }

    @Test
    fun createLiveWallpaperModelFromWallpaperInstance_applyToHome() {
        val sourceInfo = WallpaperInfoUtils.createWallpaperInfo(context)
        val sourceDescription =
            WallpaperDescription.Builder()
                .setId("id")
                .setComponent(sourceInfo.component)
                .setContent(
                    PersistableBundle().apply {
                        putString("picker_metadata_collection_id", "collectionId")
                    }
                )
                .build()
        val sourceInstance = WallpaperInstance(sourceInfo, sourceDescription)

        val wallpaperModel =
            CurrentWallpaperModelUtils.createCurrentLiveWallpaperModelFromInstance(
                context,
                sourceInstance,
                WallpaperManager.FLAG_SYSTEM,
            ) as WallpaperModel.LiveWallpaperModel

        assertThat(wallpaperModel.commonWallpaperData.id.uniqueId).isEqualTo("NewYorkWallpaper")
        assertThat(wallpaperModel.commonWallpaperData.id.componentName)
            .isEqualTo(sourceInfo.component)
        assertThat(wallpaperModel.commonWallpaperData.id.collectionId).isEqualTo("collectionId")
        assertThat(wallpaperModel.commonWallpaperData.title).isEqualTo("nonLocalizedLabel")
        assertThat(wallpaperModel.commonWallpaperData.attributions)
            .isEqualTo(listOf("nonLocalizedLabel", "author", "description"))
        assertThat(wallpaperModel.commonWallpaperData.exploreActionUrl).isEqualTo("contextUri")
        assertThat(wallpaperModel.commonWallpaperData.placeholderColorInfo.wallpaperColors).isNull()
        assertThat(wallpaperModel.commonWallpaperData.placeholderColorInfo.placeholderColor)
            .isEqualTo(0)
        // The FakeWallpaperModelConversionHelper provides a TestAsset for the thumbAsset.
        assertThat(wallpaperModel.commonWallpaperData.thumbAsset)
            .isInstanceOf(TestAsset::class.java)
        assertThat(wallpaperModel.commonWallpaperData.destination)
            .isEqualTo(Destination.APPLIED_TO_SYSTEM)
        assertThat(wallpaperModel.liveWallpaperData.groupName).isEqualTo("")
        assertThat(wallpaperModel.liveWallpaperData.systemWallpaperInfo).isEqualTo(sourceInfo)
        assertThat(wallpaperModel.liveWallpaperData.isTitleVisible).isTrue()
        assertThat(wallpaperModel.liveWallpaperData.isApplied).isTrue()
    }
}
