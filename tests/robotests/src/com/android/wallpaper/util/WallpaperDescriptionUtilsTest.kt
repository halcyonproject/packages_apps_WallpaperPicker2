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
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.PersistableBundle
import android.service.wallpaper.WallpaperService
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.testing.ShadowWallpaperInfo
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.WallpaperInfoUtils
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getCollectionId
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getEffects
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getPlaceHolderColor
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getUniqueId
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.updateMetadata
import com.android.wallpaper.util.WallpaperDescriptionUtils.Companion.getImageUrl
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(shadows = [ShadowWallpaperInfo::class])
@RunWith(RobolectricTestRunner::class)
class WallpaperDescriptionUtilsTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var testInjector: TestInjector

    lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)

        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun updateMetadata_addsFields() {
        val content = PersistableBundle()
        val collectionId = "collectionId"
        val placeHolderColor = 12345678
        val uniqueId = "uniqueId"
        val effects = "effects"
        val imageUrl = "imageUrl"
        assertThat(getCollectionId(content)).isNull()
        assertThat(getPlaceHolderColor(content)).isNull()
        assertThat(getEffects(content)).isNull()
        assertThat(getImageUrl(content)).isNull()

        updateMetadata(content, collectionId, placeHolderColor, uniqueId, effects, imageUrl)

        assertThat(getCollectionId(content)).isEqualTo(collectionId)
        assertThat(getPlaceHolderColor(content)).isEqualTo(placeHolderColor)
        assertThat(getUniqueId(content)).isEqualTo(uniqueId)
        assertThat(getEffects(content)).isEqualTo(effects)
        assertThat(getImageUrl(content)).isEqualTo(imageUrl)
    }

    @Test
    fun updateMetadata_changesFields() {
        val content =
            updateMetadata(
                content = PersistableBundle(),
                collectionId = "bogus",
                placeHolderColor = -1,
                uniqueId = "",
                effects = "bogus",
                imageUrl = null,
            )
        val collectionId = "collectionId"
        val placeHolderColor = 12345678
        val uniqueId = "uniqueId"
        val effects = "effects"
        val imageUrl = "imageUrl"
        assertThat(getCollectionId(content)).isNotNull()
        assertThat(getPlaceHolderColor(content)).isNotNull()
        assertThat(getEffects(content)).isNotNull()
        assertThat(getImageUrl(content)).isNull()

        updateMetadata(content, collectionId, placeHolderColor, uniqueId, effects, imageUrl)

        assertThat(getCollectionId(content)).isEqualTo(collectionId)
        assertThat(getPlaceHolderColor(content)).isEqualTo(placeHolderColor)
        assertThat(getUniqueId(content)).isEqualTo(uniqueId)
        assertThat(getEffects(content)).isEqualTo(effects)
        assertThat(getImageUrl(content)).isEqualTo(imageUrl)
    }

    @Test
    fun liveWallpaper_toDescription_succeedsWithAllFields() {
        // Make sure that existing WallpaperDescription fields are preferred over LiveWallpaperModel
        // equivalents
        val componentName = ComponentName("package", "class")
        val contextUri = Uri.parse("uri://context")
        val content = PersistableBundle().apply { putString("key1", "value1") }
        val sourceDescription =
            WallpaperDescription.Builder()
                .setComponent(componentName)
                .setId("id")
                .setTitle("title")
                .setDescription(listOf("line1", "line2"))
                .setContextUri(contextUri)
                .setContent(content)
                .build()
        val wallpaperInfo = WallpaperInfoUtils.createWallpaperInfo(context, componentName)
        val liveWallpaperModel =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = "uniqueId",
                collectionId = "collectionId",
                systemWallpaperInfo = wallpaperInfo,
                description = sourceDescription,
                placeholderColor = 123,
                effectNames = "effects",
                attribution = listOf("ignored1", "ignored2"),
                actionUrl = "ignored",
            )

        val description = liveWallpaperModel.toDescription()

        assertThat(description.component).isEqualTo(componentName)
        assertThat(description.id).isEqualTo("id")
        assertThat(getUniqueId(description.content)).isEqualTo("uniqueId")
        assertThat(getCollectionId(description.content)).isEqualTo("collectionId")
        assertThat(getPlaceHolderColor(description.content)).isEqualTo(123)
        assertThat(getEffects(description.content)).isEqualTo("effects")
        assertThat(description.title).isEqualTo("title")
        assertThat(description.description).containsExactly("line1", "line2").inOrder()
        assertThat(description.contextUri).isEqualTo(contextUri)
        assertThat(description.content.containsKey("key1")).isTrue()
        assertThat(description.content.getString("key1")).isEqualTo("value1")
    }

    @Test
    fun liveWallpaper_toDescription_succeedsWithMinimumFields() {
        // Make sure that existing LiveWallpaperModel values are used when not available from
        // WallpaperDescription
        val componentName = ComponentName("package", "class")
        val contextUri = Uri.parse("uri://context")
        val sourceDescription =
            WallpaperDescription.Builder().setComponent(componentName).setId("id").build()
        val wallpaperInfo = WallpaperInfoUtils.createWallpaperInfo(context, componentName)
        val liveWallpaperModel =
            WallpaperModelUtils.getLiveWallpaperModel(
                wallpaperId = "uniqueId",
                collectionId = "collectionId",
                systemWallpaperInfo = wallpaperInfo,
                description = sourceDescription,
                placeholderColor = 123,
                effectNames = "effects",
                attribution = listOf("title", "line1", "line2"),
                actionUrl = contextUri.toString(),
            )

        val description = liveWallpaperModel.toDescription()

        assertThat(description.component).isEqualTo(componentName)
        assertThat(description.id).isEqualTo("id")
        assertThat(getUniqueId(description.content)).isEqualTo("uniqueId")
        assertThat(getCollectionId(description.content)).isEqualTo("collectionId")
        assertThat(getPlaceHolderColor(description.content)).isEqualTo(123)
        assertThat(getEffects(description.content)).isEqualTo("effects")
        assertThat(description.title).isEqualTo("title")
        assertThat(description.description).containsExactly("line1", "line2").inOrder()
        assertThat(description.contextUri).isEqualTo(contextUri)
    }

    @Test
    fun staticWallpaper_toDescription_succeeds() {
        val contextUri = Uri.parse("uri://context")
        val cropRect = Rect(1, 2, 3, 4)
        val staticWallpaperModel =
            WallpaperModelUtils.getStaticWallpaperModel(
                wallpaperId = "uniqueId",
                collectionId = "collectionId",
                title = "ignored",
                placeholderColor = 123,
                attribution = listOf("title", "line1", "line2"),
                actionUrl = contextUri.toString(),
                imageWallpaperUri = Uri.parse("uri://image"),
            )

        val description =
            staticWallpaperModel.toDescription("id", mapOf(Point(100, 200) to cropRect))

        assertThat(description.id).isEqualTo("id")
        assertThat(getUniqueId(description.content)).isEqualTo("uniqueId")
        assertThat(description.component).isEqualTo(null)
        assertThat(getCollectionId(description.content)).isEqualTo("collectionId")
        assertThat(getPlaceHolderColor(description.content)).isEqualTo(123)
        assertThat(getImageUrl(description.content)).isEqualTo("uri://image")
        assertThat(description.title).isEqualTo("title")
        assertThat(description.description).containsExactly("line1", "line2").inOrder()
        assertThat(description.contextUri).isEqualTo(contextUri)
        assertThat(description.cropHints.contains(WallpaperManager.ORIENTATION_PORTRAIT)).isTrue()
        assertThat(description.cropHints.get(WallpaperManager.ORIENTATION_PORTRAIT))
            .isEqualTo(cropRect)
    }

    @Test
    fun createWallpaperInfoFromDescription_succeeds() {
        setupResolveInfo()
        val componentName = ComponentName(PACKAGE_NAME, CLASS_NAME)
        val wallpaperDescription =
            WallpaperDescription.Builder().setComponent(componentName).build()

        val wallpaperInfo =
            WallpaperDescriptionUtils.createWallpaperInfoFromDescription(
                context,
                wallpaperDescription,
            )

        assertThat(wallpaperInfo.component).isEqualTo(componentName)
    }

    @Test
    fun createWallpaperInfoFromDescription_emptyPackageName_succeeds() {
        setupResolveInfo()
        val componentName = ComponentName("", PACKAGE_NAME + "/" + CLASS_NAME)
        val wallpaperDescription =
            WallpaperDescription.Builder().setComponent(componentName).build()

        val wallpaperInfo =
            WallpaperDescriptionUtils.createWallpaperInfoFromDescription(
                context,
                wallpaperDescription,
            )

        assertThat(wallpaperInfo.component).isEqualTo(ComponentName(PACKAGE_NAME, CLASS_NAME))
    }

    @Test
    fun createWallpaperInfoFromDescription_invalidComponentName_throwsException() {
        setupResolveInfo()
        val wallpaperDescription = WallpaperDescription.Builder().build()

        assertThrows(IllegalArgumentException::class.java) {
            WallpaperDescriptionUtils.createWallpaperInfoFromDescription(
                context,
                wallpaperDescription,
            )
        }
    }

    @Test
    fun createWallpaperInfoFromDescription_serviceNotFound_throwsException() {
        val componentName = ComponentName(PACKAGE_NAME, CLASS_NAME)
        val wallpaperDescription =
            WallpaperDescription.Builder().setComponent(componentName).build()

        assertThrows(IllegalArgumentException::class.java) {
            WallpaperDescriptionUtils.createWallpaperInfoFromDescription(
                context,
                wallpaperDescription,
            )
        }
    }

    @Test
    fun createWallpaperInfoFromDescription_invalidServiceName_throwsException() {
        setupResolveInfo()
        val componentName = ComponentName("", "invalidName")
        val wallpaperDescription =
            WallpaperDescription.Builder().setComponent(componentName).build()

        assertThrows(IllegalArgumentException::class.java) {
            WallpaperDescriptionUtils.createWallpaperInfoFromDescription(
                context,
                wallpaperDescription,
            )
        }
    }

    @Test
    fun wallpaperDesctiptionToLiveWallpaperModel_succeeds() {
        setupResolveInfo()
        val componentName = ComponentName(PACKAGE_NAME, CLASS_NAME)
        val wallpaperDescription =
            WallpaperDescription.Builder()
                .setComponent(componentName)
                .setId("id")
                .setContent(PersistableBundle().apply { putString("EffectName", "effect") })
                .build()

        var liveWallpaperModel = wallpaperDescription.toLiveWallpaperModel(context)

        assertThat(liveWallpaperModel).isNotNull()
        liveWallpaperModel = liveWallpaperModel!!
        assertThat(liveWallpaperModel.commonWallpaperData.id.componentName).isEqualTo(componentName)
        assertThat(liveWallpaperModel.commonWallpaperData.id.uniqueId)
            .isEqualTo(CLASS_NAME + "_" + "id")
        assertThat(liveWallpaperModel.commonWallpaperData.id.collectionId)
            .isEqualTo("image_wallpapers")
        assertThat(liveWallpaperModel.commonWallpaperData.title).isEqualTo("null")
        assertThat(liveWallpaperModel.commonWallpaperData.attributions).isEqualTo(listOf("null"))
        assertThat(liveWallpaperModel.commonWallpaperData.exploreActionUrl).isNull()
        assertThat(liveWallpaperModel.commonWallpaperData.placeholderColorInfo.wallpaperColors)
            .isNull()
        assertThat(liveWallpaperModel.commonWallpaperData.placeholderColorInfo.placeholderColor)
            .isEqualTo(Color.TRANSPARENT)
        assertThat(liveWallpaperModel.liveWallpaperData.groupName).isEqualTo("")
        assertThat(liveWallpaperModel.liveWallpaperData.systemWallpaperInfo.component)
            .isEqualTo(componentName)
        assertThat(liveWallpaperModel.liveWallpaperData.isTitleVisible).isFalse()
        assertThat(liveWallpaperModel.liveWallpaperData.isApplied).isFalse()
        assertThat(liveWallpaperModel.liveWallpaperData.isEffectWallpaper).isFalse()
        assertThat(liveWallpaperModel.liveWallpaperData.effectNames).isEqualTo("effect")
        assertThat(liveWallpaperModel.liveWallpaperData.contextDescription).isNull()
        assertThat(liveWallpaperModel.liveWallpaperData.description).isEqualTo(wallpaperDescription)
        assertThat(liveWallpaperModel.liveWallpaperData.supportsMultipleEngines).isFalse()
        assertThat(liveWallpaperModel.creativeWallpaperData).isNull()
        assertThat(liveWallpaperModel.internalLiveWallpaperData).isNull()
    }

    // Sets up the ResolveInfo for the given component name. This is needed for
    // context.packageManager.queryIntentServices() to return the ResolveInfo
    // for WallpaperInfo creation.
    private fun setupResolveInfo() {
        val componentName = ComponentName(PACKAGE_NAME, CLASS_NAME)
        val resolveInfo =
            ResolveInfo().apply {
                serviceInfo = ServiceInfo()
                serviceInfo.packageName = PACKAGE_NAME
                serviceInfo.name = CLASS_NAME
                serviceInfo.flags = PackageManager.GET_META_DATA
            }
        val pm = shadowOf(context.packageManager)
        val intent =
            Intent(WallpaperService.SERVICE_INTERFACE).apply { setComponent(componentName) }
        pm.addResolveInfoForIntent(intent, resolveInfo)
    }

    companion object {
        private const val PACKAGE_NAME = "package"
        private const val CLASS_NAME = "class"
    }
}
