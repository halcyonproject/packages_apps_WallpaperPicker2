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
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Elements
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Scenes
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.SharedElements
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel

@Composable
fun ContentScope.FullWallpaperPreviewScene(
    sceneState: MutableSceneTransitionLayoutState,
    viewModel: WallpaperPreviewViewModel,
    screen: Screen,
    preview: View,
) {
    val coroutineScope = rememberCoroutineScope()
    val systemBarPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues()

    val windowSize: IntSize = LocalWindowInfo.current.containerSize
    val phoneAspectRatio: Float = windowSize.width.toFloat() / windowSize.height.toFloat()

    val onLockDispatchTouchEvent: ((event: MotionEvent) -> Unit)? by
        viewModel.onLockDispatchTouchEvent.collectAsStateWithLifecycle(null)
    val onHomeDispatchTouchEvent: ((event: MotionEvent) -> Unit)? by
        viewModel.onHomeDispatchTouchEvent.collectAsStateWithLifecycle(null)

    val onCropButtonClick: (() -> Unit)? by
        viewModel.onCropButtonClick.collectAsStateWithLifecycle(null)
    val onCancelCrop: (() -> Unit)? by viewModel.onCancelCrop.collectAsStateWithLifecycle(null)

    val handleBackNavigation = {
        onCancelCrop?.invoke()
        sceneState.setTargetScene(Scenes.SmallPreview, coroutineScope)
    }

    BackHandler { handleBackNavigation.invoke() }

    Box(
        modifier =
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event: MotionEvent? = awaitPointerEvent().motionEvent
                        if (event != null) {
                            when (screen) {
                                Screen.LOCK_SCREEN -> onLockDispatchTouchEvent?.invoke(event)
                                Screen.HOME_SCREEN -> onHomeDispatchTouchEvent?.invoke(event)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        MovableElement(
            key =
                when (screen) {
                    Screen.LOCK_SCREEN -> SharedElements.LockScreen
                    Screen.HOME_SCREEN -> SharedElements.HomeScreen
                },
            modifier = Modifier.fillMaxHeight().aspectRatio(phoneAspectRatio),
        ) {
            content { PreviewScreen(preview = preview, modifier = Modifier.fillMaxSize()) }
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
        Box(modifier = Modifier.clickable { /* Handle back */ }.padding(vertical = 4.dp)) {
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
