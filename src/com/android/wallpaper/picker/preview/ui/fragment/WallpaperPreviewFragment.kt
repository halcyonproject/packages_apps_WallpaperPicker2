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
package com.android.wallpaper.picker.preview.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.SceneTransitions
import com.android.compose.animation.scene.rememberMutableSceneTransitionLayoutState
import com.android.compose.animation.scene.transitions
import com.android.compose.theme.PlatformTheme
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.ui.view.ApplyWallpaperScreen
import com.android.wallpaper.picker.preview.ui.view.SmallWallpaperPreviewScreen
import com.android.wallpaper.picker.preview.ui.view.WallpaperPreviewHomeScreen
import com.android.wallpaper.picker.preview.ui.view.WallpaperPreviewLockScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * This fragment displays the preview of the selected wallpaper on all available workspaces and
 * device displays.
 */
@AndroidEntryPoint(AppbarFragment::class)
class WallpaperPreviewFragment : Hilt_WallpaperPreviewFragment() {

    object Scenes {
        val SmallPreview = SceneKey(debugName = "SmallPreview")
        val FullLockPreview = SceneKey(debugName = "FullLockPreview")
        val FullHomePreview = SceneKey(debugName = "FullHomePreview")
        val ApplyWallpaper = SceneKey(debugName = "ApplyWallpaper")
    }

    object Elements {
        val LockScreen = ElementKey(debugName = "LockScreen")
        val HomeScreen = ElementKey(debugName = "HomeScreen")
        val SmallPreviewTopToolbar = ElementKey(debugName = "SmallPreviewTopToolbar")
        val SmallPreviewBottomActionBar = ElementKey(debugName = "SmallPreviewBottomActionBar")
        val ApplyWallpaperTitle = ElementKey(debugName = "ApplyWallpaperTitle")
        val ApplyWallpaperLockScreenCheckbox =
            ElementKey(debugName = "ApplyWallpaperLockScreenCheckbox")
        val ApplyWallpaperHomeScreenCheckbox =
            ElementKey(debugName = "ApplyWallpaperHomeScreenCheckbox")
        val ApplyWallpaperBottomButtons = ElementKey(debugName = "ApplyWallpaperBottomButtons")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { PlatformTheme { WallpaperPreviewRootContent() } }
        }
    }

    @Composable
    fun WallpaperPreviewRootContent(modifier: Modifier = Modifier) {
        val sceneState =
            rememberMutableSceneTransitionLayoutState(
                initialScene = Scenes.SmallPreview,
                transitions = remember { sceneTransitions() },
            )
        // Pager state needs to be outside the scope of the SceneTransitionLayout so that after
        // transitioning back to the small preview scene, the selected page index can be retained.
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

        SceneTransitionLayout(state = sceneState, modifier = modifier) {
            // The order of the scene here matters. During transitions the first defined scene will
            // be drawn below the second, scene, which will be drawn below the third one, etc.
            scene(Scenes.SmallPreview) { SmallWallpaperPreviewScreen(sceneState, pagerState) }
            scene(Scenes.ApplyWallpaper, userActions = mapOf(Back to Scenes.SmallPreview)) {
                ApplyWallpaperScreen()
            }
            scene(Scenes.FullLockPreview, userActions = mapOf(Back to Scenes.SmallPreview)) {
                FullLockPreviewScreen()
            }
            scene(Scenes.FullHomePreview, userActions = mapOf(Back to Scenes.SmallPreview)) {
                FullHomePreviewScreen()
            }
        }
    }

    @Composable
    fun ContentScope.FullLockPreviewScreen() {
        val windowSize: IntSize = LocalWindowInfo.current.containerSize
        val phoneAspectRatio: Float = windowSize.width.toFloat() / windowSize.height.toFloat()
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            WallpaperPreviewLockScreen(
                modifier =
                    Modifier.element(Elements.LockScreen)
                        .fillMaxHeight()
                        .aspectRatio(phoneAspectRatio)
            )
        }
    }

    @Composable
    fun ContentScope.FullHomePreviewScreen() {
        val windowSize: IntSize = LocalWindowInfo.current.containerSize
        val phoneAspectRatio: Float = windowSize.width.toFloat() / windowSize.height.toFloat()
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            WallpaperPreviewHomeScreen(
                modifier =
                    Modifier.element(Elements.HomeScreen)
                        .fillMaxHeight()
                        .aspectRatio(phoneAspectRatio)
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewFragmentComposeContent() {
        PlatformTheme { WallpaperPreviewRootContent() }
    }

    private fun sceneTransitions(): SceneTransitions {
        return transitions {
            from(Scenes.SmallPreview, to = Scenes.ApplyWallpaper) {
                spec = spring()
                fractionRange(end = 0.7f) { fade(Elements.SmallPreviewTopToolbar) }
                fractionRange(end = 0.7f) { fade(Elements.SmallPreviewBottomActionBar) }
                fractionRange(start = 0.7f) { fade(Elements.ApplyWallpaperTitle) }
                fractionRange(start = 0.7f) { fade(Elements.ApplyWallpaperLockScreenCheckbox) }
                fractionRange(start = 0.7f) { fade(Elements.ApplyWallpaperHomeScreenCheckbox) }
                fractionRange(start = 0.7f) { fade(Elements.ApplyWallpaperBottomButtons) }
            }
            from(Scenes.SmallPreview, to = Scenes.FullLockPreview) {
                spec = spring()
                fractionRange(end = 0.7f) { fade(Elements.SmallPreviewTopToolbar) }
                fractionRange(end = 0.7f) { fade(Elements.SmallPreviewBottomActionBar) }
                fractionRange(end = 0.7f) { fade(Elements.HomeScreen) }
            }
            from(Scenes.SmallPreview, to = Scenes.FullHomePreview) {
                spec = spring()
                fractionRange(end = 0.7f) { fade(Elements.SmallPreviewTopToolbar) }
                fractionRange(end = 0.7f) { fade(Elements.SmallPreviewBottomActionBar) }
                fractionRange(end = 0.7f) { fade(Elements.LockScreen) }
            }
        }
    }
}
