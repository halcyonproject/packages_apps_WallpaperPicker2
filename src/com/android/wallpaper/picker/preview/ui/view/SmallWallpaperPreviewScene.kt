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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Elements
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Scenes
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.SharedElements
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.DeleteConfirmationDialogViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.PreviewFloatingSheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [SmallWallpaperPreviewScene] is one scene in [WallpaperPreviewFragment]'s SceneTransitionLayout.
 * It is bound to the [WallpaperPreviewFragment] and is not expected to be used somewhere else.
 */
@Composable
fun ContentScope.SmallWallpaperPreviewScene(
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    pagerState: PagerState,
    lockScreenPreview: View,
    homeScreenPreview: View,
    logger: UserEventLogger,
    onFinishActivity: () -> Unit,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val colorScheme: ColorScheme = MaterialTheme.colorScheme
    val systemBarPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues()

    val onLockSmallPreviewClicked: (() -> Unit)? by
        viewModel
            .onSmallPreviewClicked(
                screen = Screen.LOCK_SCREEN,
                deviceDisplayType = DeviceDisplayType.SINGLE,
                navigate = {
                    sceneState.setTargetScene(
                        Scenes.FullLockPreview,
                        animationScope = coroutineScope,
                    )
                },
            )
            .collectAsStateWithLifecycle(null)
    val onHomeSmallPreviewClicked: (() -> Unit)? by
        viewModel
            .onSmallPreviewClicked(
                screen = Screen.HOME_SCREEN,
                deviceDisplayType = DeviceDisplayType.SINGLE,
                navigate = {
                    sceneState.setTargetScene(
                        Scenes.FullHomePreview,
                        animationScope = coroutineScope,
                    )
                },
            )
            .collectAsStateWithLifecycle(null)

    // Sync view model's smallPreviewSelectedTab with the pager state
    when (pagerState.currentPage) {
        0 -> viewModel.setSmallPreviewSelectedTab(Screen.LOCK_SCREEN)
        1 -> viewModel.setSmallPreviewSelectedTab(Screen.HOME_SCREEN)
    }

    val actionsViewModel: PreviewActionsViewModel = viewModel.previewActionsViewModel
    val previewFloatingSheetViewModel: PreviewFloatingSheetViewModel? by
        actionsViewModel.previewFloatingSheetViewModel.collectAsStateWithLifecycle(null)
    /** [INFORMATION] */
    val isInformationButtonVisible: Boolean by
        actionsViewModel.isInformationVisible.collectAsStateWithLifecycle(false)
    val onInformationClicked: (() -> Unit)? by
        actionsViewModel.onInformationClicked.collectAsStateWithLifecycle(null)
    /** [DOWNLOAD] */
    val isDownloadButtonVisible: Boolean by
        actionsViewModel.isDownloadVisible.collectAsStateWithLifecycle(false)
    val isDownloading: Boolean by actionsViewModel.isDownloading.collectAsStateWithLifecycle(false)
    val onDownloadButtonClicked: (() -> Unit)? by
        actionsViewModel.onDownloadButtonClicked.collectAsStateWithLifecycle(null)
    /** [DELETE] */
    val isDeleteButtonVisible: Boolean by
        actionsViewModel.isDeleteVisible.collectAsStateWithLifecycle(false)
    val onDeleteButtonClicked: (() -> Unit)? by
        actionsViewModel.onDeleteClicked.collectAsStateWithLifecycle(null)
    val deleteConfirmationDialogViewModel: DeleteConfirmationDialogViewModel? by
        actionsViewModel.deleteConfirmationDialogViewModel.collectAsStateWithLifecycle(null)
    val isDeleting: Boolean by actionsViewModel.isDeleting.collectAsStateWithLifecycle(false)

    Column {
        // Status bar space to avoid the top toolbar overlapping with the status bar.
        Spacer(modifier = Modifier.height(systemBarPadding.calculateTopPadding()))

        SmallPreviewTopToolbar(
            viewModel = viewModel,
            modifier = Modifier.element(Elements.SmallPreviewTopToolbar),
            sceneState = sceneState,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PreviewPager(
            modifier = Modifier.fillMaxWidth().weight(1f),
            pagerState = pagerState,
            lockScreenPreview = lockScreenPreview,
            homeScreenPreview = homeScreenPreview,
            onPreviewClick = { screen ->
                when (screen) {
                    Screen.LOCK_SCREEN -> {
                        if (pagerState.currentPage != 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                        onLockSmallPreviewClicked?.invoke()
                    }
                    Screen.HOME_SCREEN -> {
                        if (pagerState.currentPage != 1) {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                        onHomeSmallPreviewClicked?.invoke()
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier =
                Modifier.element(Elements.SmallPreviewBottomActionBar)
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isInformationButtonVisible) {
                Box(modifier = Modifier.padding(vertical = 4.dp).padding(end = 16.dp)) {
                    IconButton(
                        modifier = Modifier.size(56.dp),
                        onClick = { onInformationClicked?.invoke() },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = colorScheme.surfaceContainerHighest
                            ),
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_info_filled),
                            contentDescription = stringResource(R.string.tab_info),
                            tint = colorScheme.primary,
                        )
                    }
                }
            }

            if (isDownloadButtonVisible) {
                Box(modifier = Modifier.padding(vertical = 4.dp).padding(end = 16.dp)) {
                    IconButton(
                        modifier = Modifier.size(56.dp),
                        onClick = { onDownloadButtonClicked?.invoke() },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = colorScheme.surfaceContainerHighest
                            ),
                        shape = CircleShape,
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        } else {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(R.drawable.ic_file_download_filled),
                                contentDescription =
                                    stringResource(R.string.bottom_action_bar_download),
                                tint = colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (isDeleteButtonVisible) {
                Box(modifier = Modifier.padding(vertical = 4.dp).padding(end = 16.dp)) {
                    IconButton(
                        modifier = Modifier.size(56.dp),
                        onClick = { onDeleteButtonClicked?.invoke() },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = colorScheme.surfaceContainerHighest
                            ),
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_delete_filled),
                            contentDescription = stringResource(R.string.delete_live_wallpaper),
                            tint = colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Bottom handle bar space to avoid the bottom icon overlapping with the handle bar.
        Spacer(modifier = Modifier.height(systemBarPadding.calculateBottomPadding()))

        previewFloatingSheetViewModel?.let {
            PreviewBottomSheet(
                previewFloatingSheetViewModel = it,
                logger = logger,
                onBottomSheetCollapsed = {
                    viewModel.previewActionsViewModel.onFloatingSheetCollapsed()
                },
            )
        }

        // Delete wallpaper alert dialog
        deleteConfirmationDialogViewModel?.let {
            AlertDialog(
                onDismissRequest = {
                    if (!isDeleting) {
                        it.onDismiss.invoke()
                    }
                },
                title = { Text(text = stringResource(R.string.delete_live_wallpaper)) },
                text = { Text(text = stringResource(R.string.delete_wallpaper_confirmation)) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isDeleting) {
                                coroutineScope.launch {
                                    it.onDelete?.invoke()
                                    // After deletion completes, finish the Activity to return to
                                    // the previous screen.
                                    onFinishActivity.invoke()
                                }
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).alpha(if (isDeleting) 1f else 0f),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Text(
                                text = stringResource(R.string.delete_live_wallpaper),
                                modifier = Modifier.alpha(if (isDeleting) 0f else 1f),
                            )
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { it.onDismiss.invoke() }, enabled = !isDeleting) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun ContentScope.PreviewPager(
    pagerState: PagerState,
    lockScreenPreview: View,
    homeScreenPreview: View,
    onPreviewClick: (screen: Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val phoneAspectRatio: Float =
        LocalWindowInfo.current.containerSize.width.toFloat() /
            LocalWindowInfo.current.containerSize.height.toFloat()

    BoxWithConstraints(modifier) {
        val minHorizontalPadding: Dp = 48.dp
        val minVerticalPadding: Dp = 8.dp
        val availableWidth: Dp = maxWidth - minHorizontalPadding * 2
        val availableHeight: Dp = maxHeight - minVerticalPadding * 2
        val pagerAspectRatio: Float = availableWidth / availableHeight
        val isPreviewFillMaxHeight: Boolean = pagerAspectRatio > phoneAspectRatio
        val pageWidth: Dp =
            if (isPreviewFillMaxHeight) availableHeight * phoneAspectRatio else availableWidth
        val pageHeight: Dp =
            if (isPreviewFillMaxHeight) availableHeight else availableWidth / phoneAspectRatio
        // Use content padding to center the selected page horizontally and vertically
        val contentPaddingHorizontal: Dp = (maxWidth - pageWidth) / 2
        val contentPaddingVertical: Dp = (maxHeight - pageHeight) / 2

        HorizontalPager(
            pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    horizontal = contentPaddingHorizontal,
                    vertical = contentPaddingVertical,
                ),
            pageSpacing = 12.dp,
        ) { page ->
            when (page) {
                0 ->
                    MovableElement(
                        key = SharedElements.LockScreen,
                        Modifier.size(pageWidth, pageHeight),
                    ) {
                        content {
                            PreviewScreen(
                                preview = lockScreenPreview,
                                modifier =
                                    Modifier.fillMaxSize().clickable {
                                        onPreviewClick.invoke(Screen.LOCK_SCREEN)
                                    },
                            )
                        }
                    }

                1 ->
                    MovableElement(
                        key = SharedElements.HomeScreen,
                        Modifier.size(pageWidth, pageHeight),
                    ) {
                        content {
                            PreviewScreen(
                                preview = homeScreenPreview,
                                modifier =
                                    Modifier.fillMaxSize().clickable {
                                        onPreviewClick.invoke(Screen.HOME_SCREEN)
                                    },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun SmallPreviewTopToolbar(
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    modifier: Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    val isNextButtonVisible: Boolean by
        viewModel.isSetWallpaperButtonVisible.collectAsStateWithLifecycle(false)
    val onNextButtonClicked: (() -> Unit)? by
        viewModel.onNextButtonClicked.collectAsStateWithLifecycle(null)

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier.clickable { /* Handle back */ }
                    .padding(vertical = 4.dp)
                    .padding(end = 16.dp)
        ) {
            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = {},
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = colorScheme.surfaceContainerHighest
                    ),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_nav_back_24dp),
                    contentDescription = stringResource(R.string.bottom_action_bar_back),
                    tint = colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.preview),
            fontSize = 20.sp,
            color = colorScheme.onSurface,
        )
        if (isNextButtonVisible) {
            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                Button(
                    modifier = Modifier.height(40.dp),
                    onClick = {
                        onNextButtonClicked?.invoke()
                        sceneState.setTargetScene(
                            Scenes.ApplyWallpaper,
                            animationScope = coroutineScope,
                        )
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary,
                        ),
                    shape = RoundedCornerShape(percent = 50),
                ) {
                    Text(text = stringResource(R.string.next_page_content_description))
                }
            }
        }
    }
}
