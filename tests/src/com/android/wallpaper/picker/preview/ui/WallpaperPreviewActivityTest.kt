/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.wallpaper.picker.preview.ui

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestStaticWallpaperInfo
import com.android.wallpaper.testing.WallpaperModelUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
/**
 * Tests for [WallpaperPreviewActivity].
 *
 * As of ag/37291715 this test is known to fail in Android Studio for AOSP variants because the
 * DisplayInfo constructor is not visible (it's marked @UnsupportedAppUsage). Not sure why this only
 * affects Studio, but it's probably the "platform_apis: true" directive in tests/Android.bp and/or
 * differences in APK signing between Soong, Studio AOSP, and Studio Google.
 */
class WallpaperPreviewActivityTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var persistentRepository: PersistentWallpaperModelRepository
    @Inject lateinit var testInjector: TestInjector

    private val testStaticWallpaperInfo =
        TestStaticWallpaperInfo(TestStaticWallpaperInfo.COLOR_DEFAULT).setWallpaperAttributions()

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)
    }

    @Test
    fun showsNavHostFragment() {
        val startIntent =
            WallpaperPreviewActivity.intentBuilder(context, false)
                .wallpaperInfo(testStaticWallpaperInfo)
                .build()
        val scenario: ActivityScenario<WallpaperPreviewActivity> =
            ActivityScenario.launch(startIntent)

        scenario.onActivity { activity ->
            val previews =
                activity.supportFragmentManager.fragments.filterIsInstance<NavHostFragment>()
            assertThat(previews).hasSize(1)
        }
    }

    @Test
    fun modelFromIntentInfo_isCorrect() {
        val startIntent =
            WallpaperPreviewActivity.intentBuilder(context, false)
                .wallpaperInfo(testStaticWallpaperInfo)
                .build()
        val scenario: ActivityScenario<WallpaperPreviewActivity> =
            ActivityScenario.launch(startIntent)

        scenario.onActivity { activity ->
            val activityScopeEntryPoint =
                EntryPointAccessors.fromActivity(
                    activity,
                    WallpaperPreviewTestActivityScopeEntryPoint::class.java,
                )
            val wallpaperPreviewRepository = activityScopeEntryPoint.wallpaperPreviewRepository()
            val model = wallpaperPreviewRepository.wallpaperModel.value
            assertThat(model).isNotNull()
            model ?: return@onActivity
            assertThat(model.commonWallpaperData.attributions).isNotNull()
            assertThat(model.commonWallpaperData.attributions).isNotEmpty()
            // We don't need to check all the fields (for now), just confirm conversion happened
            assertThat(model.commonWallpaperData.attributions!![0]).isEqualTo("Title")

            val previews =
                activity.supportFragmentManager.fragments.filterIsInstance<NavHostFragment>()
            assertThat(previews).hasSize(1)
        }
    }

    @Test
    fun modelFromPersistentRepository_isCorrect() {
        val requestedModel =
            WallpaperModelUtils.getStaticWallpaperModel(
                wallpaperId = "wallpaperId",
                collectionId = "collectionId",
            )
        persistentRepository.setWallpaperModel(requestedModel)
        val scenario: ActivityScenario<WallpaperPreviewActivity> =
            ActivityScenario.launch(WallpaperPreviewActivity::class.java)

        scenario.onActivity { activity ->
            val activityScopeEntryPoint =
                EntryPointAccessors.fromActivity(
                    activity,
                    WallpaperPreviewTestActivityScopeEntryPoint::class.java,
                )
            val wallpaperPreviewRepository = activityScopeEntryPoint.wallpaperPreviewRepository()
            val actualModel = wallpaperPreviewRepository.wallpaperModel.value
            assertThat(actualModel).isNotNull()
            actualModel ?: return@onActivity
            assertThat(actualModel.commonWallpaperData.id.wallpaperId)
                .isEqualTo(requestedModel.commonWallpaperData.id.wallpaperId)
        }
    }

    @Test
    fun modelIsCorrect_afterRecreate() {
        val requestedModel =
            WallpaperModelUtils.getStaticWallpaperModel(
                wallpaperId = "wallpaperId",
                collectionId = "collectionId",
            )
        persistentRepository.setWallpaperModel(requestedModel)
        val scenario: ActivityScenario<WallpaperPreviewActivity> =
            ActivityScenario.launch(WallpaperPreviewActivity::class.java)

        scenario.recreate()

        scenario.onActivity { activity ->
            val activityScopeEntryPoint =
                EntryPointAccessors.fromActivity(
                    activity,
                    WallpaperPreviewTestActivityScopeEntryPoint::class.java,
                )
            val wallpaperPreviewRepository = activityScopeEntryPoint.wallpaperPreviewRepository()
            val actualModel = wallpaperPreviewRepository.wallpaperModel.value
            assertThat(actualModel).isNotNull()
            actualModel ?: return@onActivity
            assertThat(actualModel.commonWallpaperData.id.wallpaperId)
                .isEqualTo(requestedModel.commonWallpaperData.id.wallpaperId)
        }
    }

    @Test
    fun showsToastWhenMissingWallpaper() {
        val model =
            WallpaperModelUtils.getStaticWallpaperModel(
                wallpaperId = "wallpaperId",
                collectionId = "collectionId",
            )
        // The first Activity launch will succeed because it reads the model value from the
        // persistent repository
        persistentRepository.setWallpaperModel(model)
        val scenario: ActivityScenario<WallpaperPreviewActivity> =
            ActivityScenario.launch(WallpaperPreviewActivity::class.java)

        scenario.onActivity { activity ->
            val previews =
                activity.supportFragmentManager.fragments.filterIsInstance<NavHostFragment>()
            assertThat(previews).hasSize(1)
        }

        // The second Activity launch will fail because the first launch clears the persistent
        // repository. We can't test for the presence of the toast message, but we can be sure the
        // Activity finishes without raising an error.
        val scenario2 = ActivityScenario.launch(WallpaperPreviewActivity::class.java)
        assertThat(scenario2.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    private fun TestStaticWallpaperInfo.setWallpaperAttributions(): WallpaperInfo {
        setAttributions(listOf("Title", "Subtitle 1", "Subtitle 2"))
        setCollectionId("collectionStatic")
        setWallpaperId("wallpaperStatic")
        setActionUrl("http://google.com")
        return this
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface WallpaperPreviewTestActivityScopeEntryPoint {
        fun wallpaperPreviewRepository(): WallpaperPreviewRepository
    }
}
