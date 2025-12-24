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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.FOLDED
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.SINGLE
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.UNFOLDED
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
private val PAGE_SPACING = 16.dp

/**
 * The preview pager that is used in [SmallWallpaperPreviewScene]. It is used for the case when the
 * device is a foldable that has 2 screens (folded and unfolded).
 */
@Composable
fun ContentScope.FoldablePreviewPager(
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    pagerState: PagerState,
    lockScreenPreview: SurfaceView,
    lockScreenUnfoldedPreview: SurfaceView,
    homeScreenPreview: SurfaceView,
    homeScreenUnfoldedPreview: SurfaceView,
    displaySizes: DisplaySizes,
    modifier: Modifier = Modifier,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    val foldedRatio: Float = displaySizes.folded.x.toFloat() / displaySizes.folded.y.toFloat()
    val unfoldedRatio: Float = displaySizes.unfolded.x.toFloat() / displaySizes.unfolded.y.toFloat()
    val combinedRatio: Float = foldedRatio + unfoldedRatio

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
    fun moveSurfaceViewToTop(previewTarget: PreviewTarget) {
        val screen: Screen = previewTarget.screen
        val deviceDisplayType: DeviceDisplayType = previewTarget.deviceDisplayType
        check(previewTarget.deviceDisplayType != SINGLE) {
            "FoldablePreviewPager does not allow device display type to be SINGLE"
        }
        lockScreenPreview.apply {
            if (screen == LOCK_SCREEN && deviceDisplayType == FOLDED) toFront() else toBack()
        }
        lockScreenUnfoldedPreview.apply {
            if (screen == LOCK_SCREEN && deviceDisplayType == UNFOLDED) toFront() else toBack()
        }
        homeScreenPreview.apply {
            if (screen == HOME_SCREEN && deviceDisplayType == FOLDED) toFront() else toBack()
        }
        homeScreenUnfoldedPreview.apply {
            if (screen == HOME_SCREEN && deviceDisplayType == UNFOLDED) toFront() else toBack()
        }
    }

    BoxWithConstraints(modifier) {
        val availableWidth: Dp = maxWidth - MIN_HORIZONTAL_PADDING * 2 - SPACE_BETWEEN_PREVIEWS
        val availableHeight: Dp = maxHeight - MIN_VERTICAL_PADDING * 2
        val pagerAspectRatio: Float = availableWidth / availableHeight
        val isPreviewFillMaxHeight: Boolean = pagerAspectRatio > combinedRatio
        val pageWidth: Dp =
            (if (isPreviewFillMaxHeight) availableHeight * combinedRatio else availableWidth) +
                SPACE_BETWEEN_PREVIEWS
        val pageHeight: Dp =
            if (isPreviewFillMaxHeight) availableHeight else availableWidth / combinedRatio

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
            val screen: Screen = if (page == 0) LOCK_SCREEN else HOME_SCREEN
            val previewTarget: PreviewTarget =
                when (screen) {
                    LOCK_SCREEN -> PreviewTarget(LOCK_SCREEN, FOLDED)
                    HOME_SCREEN -> PreviewTarget(HOME_SCREEN, FOLDED)
                }
            val previewTargetUnfolded: PreviewTarget =
                when (screen) {
                    LOCK_SCREEN -> PreviewTarget(LOCK_SCREEN, UNFOLDED)
                    HOME_SCREEN -> PreviewTarget(HOME_SCREEN, UNFOLDED)
                }
            val onClick by
                viewModel
                    .onSmallPreviewClicked(previewTarget.screen, previewTarget.deviceDisplayType) {
                        moveSurfaceViewToTop(previewTarget)
                        sceneState.setTargetScene(
                            targetScene =
                                when (screen) {
                                    LOCK_SCREEN -> Scenes.FullLockPreview
                                    HOME_SCREEN -> Scenes.FullHomePreview
                                },
                            animationScope = coroutineScope,
                        )
                    }
                    .collectAsStateWithLifecycle(null)
            val onClickUnfolded by
                viewModel
                    .onSmallPreviewClicked(previewTarget.screen, previewTarget.deviceDisplayType) {
                        moveSurfaceViewToTop(previewTargetUnfolded)
                        sceneState.setTargetScene(
                            targetScene =
                                when (screen) {
                                    LOCK_SCREEN -> Scenes.FullLockUnfoldedPreview
                                    HOME_SCREEN -> Scenes.FullHomeUnfoldedPreview
                                },
                            animationScope = coroutineScope,
                        )
                    }
                    .collectAsStateWithLifecycle(null)

            Column(
                modifier = Modifier.size(pageWidth, pageHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(SPACE_BETWEEN_PREVIEWS, Alignment.CenterHorizontally),
                ) {
                    // The folded preview
                    MovableElement(
                        key =
                            when (screen) {
                                LOCK_SCREEN -> SharedElements.LockScreen
                                HOME_SCREEN -> SharedElements.HomeScreen
                            },
                        modifier = Modifier.fillMaxHeight().aspectRatio(foldedRatio),
                    ) {
                        content {
                            PreviewScreen(
                                preview =
                                    when (screen) {
                                        LOCK_SCREEN -> lockScreenPreview
                                        HOME_SCREEN -> homeScreenPreview
                                    },
                                viewModel = viewModel,
                                previewTarget = previewTarget,
                                modifier =
                                    Modifier.fillMaxSize().clickable {
                                        if (pagerState.currentPage != page) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(page)
                                            }
                                        }
                                        onClick?.invoke()
                                    },
                            )
                        }
                    }
                    // The unfolded preview
                    MovableElement(
                        key =
                            when (screen) {
                                LOCK_SCREEN -> SharedElements.LockScreenUnfolded
                                HOME_SCREEN -> SharedElements.HomeScreenUnfolded
                            },
                        modifier = Modifier.fillMaxHeight().aspectRatio(unfoldedRatio),
                    ) {
                        content {
                            PreviewScreen(
                                preview =
                                    when (screen) {
                                        LOCK_SCREEN -> lockScreenUnfoldedPreview
                                        HOME_SCREEN -> homeScreenUnfoldedPreview
                                    },
                                viewModel = viewModel,
                                previewTarget = previewTargetUnfolded,
                                modifier =
                                    Modifier.fillMaxSize().clickable {
                                        if (pagerState.currentPage != page) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(page)
                                            }
                                        }
                                        onClickUnfolded?.invoke()
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
