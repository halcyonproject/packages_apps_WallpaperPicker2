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

package com.android.wallpaper.picker.preview.ui.view

import android.view.MotionEvent
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.FOLDED
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.SINGLE
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.UNFOLDED
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Elements
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Scenes
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.SharedElements
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.DisplaySizes
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.PreviewTarget

@Composable
fun ContentScope.FullWallpaperPreviewScene(
    sceneState: MutableSceneTransitionLayoutState,
    viewModel: WallpaperPreviewViewModel,
    previewTarget: PreviewTarget,
    displaySizes: DisplaySizes,
    preview: SurfaceView,
    onDispatchTouchEvent: ((event: MotionEvent) -> Unit)?,
) {
    val coroutineScope = rememberCoroutineScope()
    val systemBarPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues()

    val onCropButtonClick: (() -> Unit)? by
        viewModel.onCropButtonClick.collectAsStateWithLifecycle(null)
    val onCancelCrop: (() -> Unit)? by viewModel.onCancelCrop.collectAsStateWithLifecycle(null)

    val handleBackNavigation = {
        onCancelCrop?.invoke()
        sceneState.setTargetScene(Scenes.SmallPreview, coroutineScope)
    }

    val windowSize: IntSize = LocalWindowInfo.current.containerSize
    val currentScreenAspectRatio: Float = windowSize.width.toFloat() / windowSize.height.toFloat()
    val previewAspectRatio: Float =
        when (previewTarget.deviceDisplayType) {
            SINGLE -> displaySizes.single.x.toFloat() / displaySizes.single.y.toFloat()
            FOLDED -> displaySizes.folded.x.toFloat() / displaySizes.folded.y.toFloat()
            UNFOLDED -> displaySizes.unfolded.x.toFloat() / displaySizes.unfolded.y.toFloat()
        }
    // The higher the ratio, the wider rectangle it will be. Ratio 1 means square.
    val isPreviewFillMaxHeight: Boolean = currentScreenAspectRatio > previewAspectRatio

    BackHandler { handleBackNavigation.invoke() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Background that fades in and out to hide and reveal the other non-selected preview(s)
        Box(
            Modifier.element(Elements.FullPreviewBackground)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Box(
            modifier =
                Modifier.then(
                        if (isPreviewFillMaxHeight) {
                            Modifier.fillMaxHeight()
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                    .fillMaxHeight()
                    .aspectRatio(previewAspectRatio)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event: MotionEvent? = awaitPointerEvent().motionEvent
                                if (event != null) {
                                    onDispatchTouchEvent?.invoke(event)
                                }
                            }
                        }
                    }
        ) {
            MovableElement(
                key =
                    when (previewTarget.screen) {
                        LOCK_SCREEN -> {
                            when (previewTarget.deviceDisplayType) {
                                SINGLE,
                                FOLDED -> SharedElements.LockScreen
                                UNFOLDED -> SharedElements.LockScreenUnfolded
                            }
                        }
                        HOME_SCREEN -> {
                            when (previewTarget.deviceDisplayType) {
                                SINGLE,
                                FOLDED -> SharedElements.HomeScreen
                                UNFOLDED -> SharedElements.HomeScreenUnfolded
                            }
                        }
                    },
                modifier = Modifier.fillMaxSize(),
            ) {
                content {
                    PreviewScreen(
                        preview = preview,
                        viewModel = viewModel,
                        previewTarget = previewTarget,
                        modifier = Modifier.fillMaxSize(),
                        applyRoundedCorner = !viewModel.isDesktopUi,
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier.element(Elements.FullPreviewTopToolbar)
                    .align(Alignment.TopCenter)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                    )
        ) {
            // Status bar space to avoid the top toolbar overlapping with the status bar.
            Spacer(modifier = Modifier.height(systemBarPadding.calculateTopPadding()))

            FullPreviewTopToolbar(
                onNavBackClick = { handleBackNavigation.invoke() },
                onCropButtonClick = {
                    onCropButtonClick?.invoke()
                    sceneState.setTargetScene(Scenes.SmallPreview, coroutineScope)
                },
            )

            // Extra space to allow the vertical gradiant background to extend more.
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FullPreviewTopToolbar(
    onNavBackClick: (() -> Unit),
    onCropButtonClick: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.padding(vertical = 4.dp)) {
            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = { onNavBackClick.invoke() },
                colors =
                    IconButtonDefaults.iconButtonColors()
                        .copy(containerColor = colorScheme.surfaceContainerHighest),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_nav_back_24dp),
                    contentDescription = stringResource(R.string.bottom_action_bar_back),
                    tint = colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(modifier = modifier.padding(vertical = 4.dp)) {
            IconButton(
                modifier = Modifier.width(52.dp).height(40.dp),
                onClick = { onCropButtonClick.invoke() },
                colors =
                    IconButtonDefaults.iconButtonColors()
                        .copy(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary,
                        ),
                shape = RoundedCornerShape(percent = 50),
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_wallpaper),
                    contentDescription =
                        stringResource(R.string.full_preview_check_button_description),
                )
            }
        }
    }
}
