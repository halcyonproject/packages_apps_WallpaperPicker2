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

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Elements
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Scenes
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.SharedElements
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [ApplyWallpaperScene] is one scene in [WallpaperPreviewFragment]'s SceneTransitionLayout. It is
 * bound to the [WallpaperPreviewFragment] and is not expected to be used somewhere else.
 */
@Composable
fun ContentScope.ApplyWallpaperScene(
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    lockScreenPreview: View,
    homeScreenPreview: View,
    onWallpaperApplied: () -> Unit,
) {
    val colorScheme: ColorScheme = MaterialTheme.colorScheme
    val systemBarPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues()
    val windowSize: IntSize = LocalWindowInfo.current.containerSize
    val phoneAspectRatio: Float = windowSize.width.toFloat() / windowSize.height.toFloat()

    val isLockScreenChecked: Boolean by
        viewModel.isLockCheckBoxChecked.collectAsStateWithLifecycle(false)
    val onLockScreenCheckChanged: (() -> Unit)? by
        viewModel.onLockCheckBoxChecked.collectAsStateWithLifecycle(null)
    val isHomeScreenChecked: Boolean by
        viewModel.isHomeCheckBoxChecked.collectAsStateWithLifecycle(false)
    val onHomeScreenCheckChanged: (() -> Unit)? by
        viewModel.onHomeCheckBoxChecked.collectAsStateWithLifecycle(null)
    val isApplyWallpaperProgressDialogVisible: Boolean by
        viewModel.isSetWallpaperProgressBarVisible.collectAsStateWithLifecycle(false)

    Box(
        modifier =
            Modifier.padding(
                top = systemBarPadding.calculateTopPadding(),
                bottom = systemBarPadding.calculateBottomPadding(),
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.element(Elements.ApplyWallpaperTitle).height(140.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.apply_wallpaper_title_text),
                        fontSize = 44.sp,
                        color = colorScheme.onSurface,
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MovableElement(
                            key = SharedElements.LockScreen,
                            modifier = Modifier.fillMaxWidth().aspectRatio(phoneAspectRatio),
                        ) {
                            content {
                                PreviewScreen(
                                    preview = lockScreenPreview,
                                    viewModel = viewModel,
                                    screen = Screen.LOCK_SCREEN,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        CheckboxWithText(
                            modifier = Modifier.element(Elements.ApplyWallpaperLockScreenCheckbox),
                            isChecked = isLockScreenChecked,
                            onCheckedChange = { onLockScreenCheckChanged?.invoke() },
                            text = stringResource(R.string.lock_screen_tab),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(Modifier.weight(1f)) {
                        MovableElement(
                            key = SharedElements.HomeScreen,
                            modifier = Modifier.fillMaxWidth().aspectRatio(phoneAspectRatio),
                        ) {
                            content {
                                PreviewScreen(
                                    preview = homeScreenPreview,
                                    viewModel = viewModel,
                                    screen = Screen.HOME_SCREEN,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        CheckboxWithText(
                            modifier = Modifier.element(Elements.ApplyWallpaperHomeScreenCheckbox),
                            isChecked = isHomeScreenChecked,
                            onCheckedChange = { onHomeScreenCheckChanged?.invoke() },
                            text = stringResource(R.string.home_screen_tab),
                        )
                    }
                }
            }
            ApplyWallpaperButtons(
                viewModel,
                sceneState,
                onWallpaperApplied,
                Modifier.element(Elements.ApplyWallpaperBottomButtons),
            )
        }

        if (isApplyWallpaperProgressDialogVisible) {
            Dialog(onDismissRequest = { /* Prevent dismissal */ }) {
                ApplyWallpaperProgressDialog()
            }
        }
    }
}

@Composable
fun ApplyWallpaperButtons(
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    onWallpaperApplied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val isApplyButtonEnabled: Boolean by
        viewModel.isApplyButtonEnabled.collectAsStateWithLifecycle(false)
    val onApplyWallpaper: (suspend () -> Unit)? by
        viewModel.setWallpaperDialogOnConfirmButtonClicked.collectAsStateWithLifecycle(null)

    val colorScheme: ColorScheme = MaterialTheme.colorScheme
    val shouldShowDesktopUi =
        BaseFlags.get(LocalContext.current).shouldShowDesktopUi(LocalContext.current)

    val onApplyButtonClicked: () -> Unit = {
        coroutineScope.launch {
            onApplyWallpaper?.invoke()
            onWallpaperApplied.invoke()
        }
    }
    val onCancelButtonClicked: () -> Unit = {
        sceneState.setTargetScene(
            targetScene = Scenes.SmallPreview,
            animationScope = coroutineScope,
        )
    }

    val applyButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
        )
    val cancelButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = colorScheme.primary,
        )
    val buttonShape = RoundedCornerShape(percent = 50)
    val cancelButtonBorder = BorderStroke(1.dp, colorScheme.outlineVariant)

    if (shouldShowDesktopUi) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f).height(56.dp),
                onClick = onApplyButtonClicked,
                colors = applyButtonColors,
                shape = buttonShape,
                enabled = isApplyButtonEnabled,
            ) {
                Text(stringResource(R.string.apply_btn))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.weight(1f).height(56.dp),
                onClick = onCancelButtonClicked,
                colors = cancelButtonColors,
                border = cancelButtonBorder,
                shape = buttonShape,
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    } else {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
        ) {
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = onApplyButtonClicked,
                colors = applyButtonColors,
                shape = buttonShape,
                enabled = isApplyButtonEnabled,
            ) {
                Text(stringResource(R.string.apply_btn))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = onCancelButtonClicked,
                colors = cancelButtonColors,
                border = cancelButtonBorder,
                shape = buttonShape,
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
fun CheckboxWithText(
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .toggleable(
                    value = isChecked,
                    onValueChange = { onCheckedChange.invoke() },
                    role = Role.Checkbox,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.padding(14.dp)
                    .size(20.dp)
                    .then(
                        if (isChecked) {
                            Modifier.background(colorScheme.primary, CircleShape)
                        } else {
                            Modifier.border(
                                width = 2.dp,
                                color = colorScheme.primary,
                                shape = CircleShape,
                            )
                        }
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Checkbox(
                modifier = Modifier.size(20.dp),
                checked = isChecked,
                onCheckedChange = null,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = Color.Transparent,
                        uncheckedColor = Color.Transparent,
                    ),
            )
        }
        Text(text = text, fontSize = 16.sp, color = colorScheme.onSurface)
    }
}

@Composable
private fun ApplyWallpaperProgressDialog() {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Row(
        modifier =
            Modifier.background(color = colorScheme.surface, shape = RoundedCornerShape(28.dp))
                .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.set_wallpaper_progress_message),
            style = typography.bodyLarge,
            color = colorScheme.onSurface,
        )
    }
}
