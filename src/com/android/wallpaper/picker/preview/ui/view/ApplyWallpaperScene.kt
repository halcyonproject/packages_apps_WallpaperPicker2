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

import android.view.SurfaceView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.SINGLE
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Elements
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Scenes
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.SharedElements
import com.android.wallpaper.picker.preview.ui.view.previewpager.FoldablePreviewPager
import com.android.wallpaper.picker.preview.ui.view.previewpager.PagerCheckBoxViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.DisplaySizes
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.PreviewTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [ApplyWallpaperScene] is one scene in [WallpaperPreviewFragment]'s SceneTransitionLayout. It is
 * bound to the [WallpaperPreviewFragment] and is not expected to be used somewhere else.
 * <pre>
 * Visual Representation:
 * +------------------------------------------+
 * |              systemBarPadding            |
 * +------------------------------------------+
 * |                Title Box                 |
 * |    (Elements.ApplyWallpaperTitle)        |
 * +------------------------------------------+
 * |                                          |
 * |            Preview Area (weight 1f)      |
 * |                                          |
 * |  If Foldable:                            |
 * |  +------------------------------------+  |
 * |  |        FoldablePreviewPager        |  |
 * |  +------------------------------------+  |
 * |                                          |
 * |  Else (Single Screen):                   |
 * |  +-------------------+   +------------+  |
 * |  |   Lock Screen     |   | Home Screen|  |
 * |  |   Preview Group   |   | Preview Grp|  |
 * |  | (CenterEnd)       |   | (CenterStrt)| |
 * |  |  +-------------+  |   | +--------+ |  |
 * |  |  | MovableElem |  | 8 | | Movable| |  |
 * |  |  | (Lock)      |  |dp | | (Home) | |  |
 * |  |  +-------------+  |   | +--------+ |  |
 * |  |  | 8dp spacer  |  |   | | 8dp sp | |  |
 * |  |  +-------------+  |   | +--------+ |  |
 * |  |  | LabelCheck  |  |   | | LabelCh| |  |
 * |  |  +-------------+  |   | +--------+ |  |
 * |  +-------------------+   +------------+  |
 * |                                          |
 * +------------------------------------------+
 * |         ApplyWallpaperButtons            |
 * | (Elements.ApplyWallpaperBottomButtons)   |
 * +------------------------------------------+
 * |        (Optional Desktop Spacer)         |
 * +------------------------------------------+
 * |             systemBarPadding             |
 * +------------------------------------------+
 * </pre>
 *
 * @param foldablePreviewPagerState We only need the pager state when [isFoldable] is true.
 */
@Composable
fun ContentScope.ApplyWallpaperScene(
    isFoldable: Boolean,
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    foldablePreviewPagerState: PagerState?,
    lockScreenPreview: SurfaceView,
    lockScreenUnfoldedPreview: SurfaceView?,
    homeScreenPreview: SurfaceView,
    homeScreenUnfoldedPreview: SurfaceView?,
    displaySizes: DisplaySizes,
    onWallpaperApplied: () -> Unit,
) {
    val colorScheme: ColorScheme = MaterialTheme.colorScheme
    val systemBarPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues()
    val displaySize = displaySizes.single
    val phoneAspectRatio: Float = displaySize.x.toFloat() / displaySize.y.toFloat()

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

    val context = LocalContext.current
    val shouldShowDesktopUi = remember { BaseFlags.get(context).shouldShowDesktopUi(context) }
    val isDesktopOrFoldable = isFoldable || shouldShowDesktopUi

    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    top = systemBarPadding.calculateTopPadding(),
                    bottom = systemBarPadding.calculateBottomPadding(),
                )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.element(Elements.ApplyWallpaperTitle)
                        .fillMaxWidth()
                        .then(
                            if (isDesktopOrFoldable) {
                                Modifier.padding(24.dp)
                            } else {
                                Modifier.padding(
                                    start = 24.dp,
                                    top = 80.dp,
                                    end = 24.dp,
                                    bottom = 16.dp,
                                )
                            }
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.apply_wallpaper_title_text),
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (isFoldable) {
                    FoldablePreviewPager(
                        viewModel = viewModel,
                        sceneState = sceneState,
                        pagerState = checkNotNull(foldablePreviewPagerState),
                        lockScreenPreview = lockScreenPreview,
                        lockScreenUnfoldedPreview = checkNotNull(lockScreenUnfoldedPreview),
                        homeScreenPreview = homeScreenPreview,
                        homeScreenUnfoldedPreview = checkNotNull(homeScreenUnfoldedPreview),
                        displaySizes = displaySizes,
                        enableNavToFullPreview = false,
                        pagerCheckBoxViewModel =
                            PagerCheckBoxViewModel(
                                isLockScreenChecked = isLockScreenChecked,
                                isHomeScreenChecked = isHomeScreenChecked,
                                onCheckChanged = { screen ->
                                    when (screen) {
                                        LOCK_SCREEN -> onLockScreenCheckChanged?.invoke()
                                        HOME_SCREEN -> onHomeScreenCheckChanged?.invoke()
                                    }
                                },
                            ),
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        // Lock screen preview group
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                            ) {
                                MovableElement(
                                    key = SharedElements.LockScreen,
                                    modifier =
                                        Modifier.weight(1f, fill = false)
                                            .aspectRatio(phoneAspectRatio),
                                ) {
                                    content {
                                        PreviewScreen(
                                            preview = lockScreenPreview,
                                            viewModel = viewModel,
                                            previewTarget = PreviewTarget(LOCK_SCREEN, SINGLE),
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }

                                LabelCheckbox(
                                    modifier =
                                        Modifier.element(Elements.ApplyWallpaperLockScreenCheckbox),
                                    isChecked = isLockScreenChecked,
                                    onCheckedChange = { onLockScreenCheckChanged?.invoke() },
                                    text = stringResource(R.string.lock_screen_tab),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Home screen preview group
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                            ) {
                                MovableElement(
                                    key = SharedElements.HomeScreen,
                                    modifier =
                                        Modifier.weight(1f, fill = false)
                                            .aspectRatio(phoneAspectRatio),
                                ) {
                                    content {
                                        PreviewScreen(
                                            preview = homeScreenPreview,
                                            viewModel = viewModel,
                                            previewTarget = PreviewTarget(HOME_SCREEN, SINGLE),
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }

                                LabelCheckbox(
                                    modifier =
                                        Modifier.element(Elements.ApplyWallpaperHomeScreenCheckbox),
                                    isChecked = isHomeScreenChecked,
                                    onCheckedChange = { onHomeScreenCheckChanged?.invoke() },
                                    text = stringResource(R.string.home_screen_tab),
                                )
                            }
                        }
                    }
                }
            }

            ApplyWallpaperButtons(
                viewModel,
                sceneState,
                onWallpaperApplied,
                isDesktopOrFoldable,
                Modifier.element(Elements.ApplyWallpaperBottomButtons),
            )

            // Need more padding at the bottom for desktop.
            if (shouldShowDesktopUi) {
                Spacer(modifier = Modifier.height(24.dp))
            }
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
    showSideBySide: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val isApplyButtonEnabled: Boolean by
        viewModel.isApplyButtonEnabled.collectAsStateWithLifecycle(false)
    val onApplyWallpaper: (suspend () -> Unit)? by
        viewModel.setWallpaperDialogOnConfirmButtonClicked.collectAsStateWithLifecycle(null)

    val colorScheme: ColorScheme = MaterialTheme.colorScheme

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

    if (showSideBySide) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
        ) {
            Button(
                modifier =
                    Modifier.weight(1f)
                        .height(56.dp)
                        .semantics { testTagsAsResourceId = true }
                        .testTag(APPLY_BUTTON_TEST_RES_ID),
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
                modifier =
                    Modifier.fillMaxWidth()
                        .height(56.dp)
                        .semantics { testTagsAsResourceId = true }
                        .testTag("com.google.android.apps.wallpaper:id/apply_button"),
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
private fun ApplyWallpaperProgressDialog() {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Row(
        modifier =
            Modifier.background(color = colorScheme.surface, shape = RoundedCornerShape(28.dp))
                .padding(24.dp)
                .semantics { testTagsAsResourceId = true }
                .testTag(PROGRESS_BAR_TEST_RES_ID),
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

private const val APPLY_BUTTON_TEST_RES_ID = "com.google.android.apps.wallpaper:id/apply_button"
private const val PROGRESS_BAR_TEST_RES_ID = "android:id/progress"
