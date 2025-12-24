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

package com.android.wallpaper.picker.preview.ui.view.previewpager

import android.view.SurfaceView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneKey
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.SINGLE
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Scenes
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.SharedElements
import com.android.wallpaper.picker.preview.ui.view.PreviewScreen
import com.android.wallpaper.picker.preview.ui.view.SmallWallpaperPreviewScene
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.DisplaySizes
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.PreviewTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val MIN_HORIZONTAL_PADDING = 48.dp
private val MIN_VERTICAL_PADDING = 8.dp
private val SPACE_BETWEEN_PREVIEWS = 8.dp
private val PAGE_SPACING = 12.dp

/**
 * The preview pager that is used in [SmallWallpaperPreviewScene]. It is used for the case when the
 * device has one single screen.
 */
@Composable
fun ContentScope.SinglePreviewPager(
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    pagerState: PagerState,
    lockScreenPreview: SurfaceView,
    homeScreenPreview: SurfaceView,
    displaySizes: DisplaySizes,
    modifier: Modifier = Modifier,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    val previewRatio: Float = displaySizes.single.x.toFloat() / displaySizes.single.y.toFloat()

    // Bring the surface view to the composition order of -1
    fun SurfaceView.toFront() {
        compositionOrder = -1
    }
    // Bring the surface view to the composition order of -2
    fun SurfaceView.toBack() {
        compositionOrder = -2
    }
    // This function will move the selected surface view to the top among all surface views. In the
    // case of overlapping, the animated, selected surface will be always on top.
    fun moveSurfaceViewToTop(screen: Screen) {
        lockScreenPreview.apply { if (screen == LOCK_SCREEN) toFront() else toBack() }
        homeScreenPreview.apply { if (screen == HOME_SCREEN) toFront() else toBack() }
    }

    BoxWithConstraints(modifier) {
        val availableWidth: Dp = maxWidth - MIN_HORIZONTAL_PADDING * 2
        val availableHeight: Dp = maxHeight - MIN_VERTICAL_PADDING * 2
        val pagerRatio: Float = availableWidth / availableHeight
        val isPreviewFillMaxHeight: Boolean = pagerRatio > previewRatio
        val pageWidth: Dp =
            if (isPreviewFillMaxHeight) availableHeight * previewRatio else availableWidth
        val pageHeight: Dp =
            if (isPreviewFillMaxHeight) availableHeight else availableWidth / previewRatio

        HorizontalPager(
            pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                // Use content padding to center the selected page horizontally and vertically
                PaddingValues(
                    horizontal = (maxWidth - pageWidth) / 2,
                    vertical = (maxHeight - pageHeight) / 2,
                ),
            pageSpacing = PAGE_SPACING,
        ) { page ->
            // Determine screen type based on page index
            val previewTarget =
                PreviewTarget(
                    screen = if (page == 0) LOCK_SCREEN else HOME_SCREEN,
                    deviceDisplayType = SINGLE,
                )
            val targetScene: SceneKey =
                if (page == 0) Scenes.FullLockPreview else Scenes.FullHomePreview
            val elementKey: MovableElementKey =
                if (page == 0) SharedElements.LockScreen else SharedElements.HomeScreen
            val preview = if (page == 0) lockScreenPreview else homeScreenPreview

            val onClick by
                viewModel
                    .onSmallPreviewClicked(previewTarget.screen, previewTarget.deviceDisplayType) {
                        moveSurfaceViewToTop(previewTarget.screen)
                        sceneState.setTargetScene(targetScene, animationScope = coroutineScope)
                    }
                    .collectAsStateWithLifecycle(null)

            MovableElement(key = elementKey, modifier = Modifier.size(pageWidth, pageHeight)) {
                content {
                    PreviewScreen(
                        preview = preview,
                        viewModel = viewModel,
                        previewTarget = previewTarget,
                        modifier =
                            Modifier.fillMaxSize().clickable {
                                if (pagerState.currentPage != page) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(page) }
                                }
                                onClick?.invoke()
                            },
                    )
                }
            }
        }
    }
}
