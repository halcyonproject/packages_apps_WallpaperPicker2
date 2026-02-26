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

import android.content.Intent
import android.view.SurfaceView
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Elements
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Scenes
import com.android.wallpaper.picker.preview.ui.view.previewpager.FoldablePreviewPager
import com.android.wallpaper.picker.preview.ui.view.previewpager.SinglePreviewPager
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.DeleteConfirmationDialogViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.DisplaySizes
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.PreviewFloatingSheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [SmallWallpaperPreviewScene] is one scene in [WallpaperPreviewFragment]'s SceneTransitionLayout.
 * It is bound to the [WallpaperPreviewFragment] and is not expected to be used somewhere else.
 */
@Composable
fun ContentScope.SmallWallpaperPreviewScene(
    isFoldable: Boolean,
    viewModel: WallpaperPreviewViewModel,
    sceneState: MutableSceneTransitionLayoutState,
    pagerState: PagerState,
    lockScreenPreview: SurfaceView,
    lockScreenUnfoldedPreview: SurfaceView?,
    homeScreenPreview: SurfaceView,
    homeScreenUnfoldedPreview: SurfaceView?,
    displaySizes: DisplaySizes,
    logger: UserEventLogger,
    onFinishActivity: () -> Unit,
    onNavigateToEditScreen: (Intent) -> Unit,
    onStartShareActivity: (Intent) -> Unit,
    extendedWallpaperEffectActivityLauncher: ActivityResultLauncher<Intent>,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val systemBarPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues()

    // Sync view model's smallPreviewSelectedTab with the pager state
    when (pagerState.currentPage) {
        0 -> viewModel.setSmallPreviewSelectedTab(LOCK_SCREEN)
        1 -> viewModel.setSmallPreviewSelectedTab(HOME_SCREEN)
    }

    val actionsViewModel: PreviewActionsViewModel = viewModel.previewActionsViewModel
    val previewFloatingSheetViewModel: PreviewFloatingSheetViewModel? by
        actionsViewModel.previewFloatingSheetViewModel.collectAsStateWithLifecycle(null)
    /** [EFFECTS] */
    val isEffectsButtonVisible: Boolean by
        actionsViewModel.isEffectsVisible.collectAsStateWithLifecycle(false)
    val onEffectsButtonClicked: ((ActivityResultLauncher<Intent>) -> Unit)? by
        actionsViewModel.onEffectsClicked.collectAsStateWithLifecycle(null)
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
    /** [EDIT] */
    val isEditButtonVisible: Boolean by
        actionsViewModel.isEditVisible.collectAsStateWithLifecycle(false)
    val editButtonIntent: Intent? by actionsViewModel.editIntent.collectAsStateWithLifecycle(null)
    /** [CUSTOMIZE] */
    val isCustomizeVisible: Boolean by
        actionsViewModel.isCustomizeVisible.collectAsStateWithLifecycle(false)
    val onCustomizeClicked: (() -> Unit)? by
        actionsViewModel.onCustomizeClicked.collectAsStateWithLifecycle(null)
    /** [SHARE] */
    val isShareButtonVisible: Boolean by
        actionsViewModel.isShareVisible.collectAsStateWithLifecycle(false)
    val shareButtonIntent: Intent? by actionsViewModel.shareIntent.collectAsStateWithLifecycle(null)
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

        if (isFoldable) {
            FoldablePreviewPager(
                viewModel = viewModel,
                sceneState = sceneState,
                pagerState = pagerState,
                lockScreenPreview = lockScreenPreview,
                lockScreenUnfoldedPreview = checkNotNull(lockScreenUnfoldedPreview),
                homeScreenPreview = homeScreenPreview,
                homeScreenUnfoldedPreview = checkNotNull(homeScreenUnfoldedPreview),
                enableNavToFullPreview = true,
                pagerCheckBoxViewModel = null,
                displaySizes = displaySizes,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            SinglePreviewPager(
                viewModel = viewModel,
                sceneState = sceneState,
                pagerState = pagerState,
                lockScreenPreview = lockScreenPreview,
                homeScreenPreview = homeScreenPreview,
                displaySizes = displaySizes,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier =
                Modifier.element(Elements.SmallPreviewBottomActionBar)
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEffectsButtonVisible) {
                ActionButtonWithLabel(
                    iconDrawableRes = R.drawable.ic_effect,
                    labelRes = R.string.tab_effects,
                    onClick = {
                        onEffectsButtonClicked?.invoke(extendedWallpaperEffectActivityLauncher)
                    },
                )
            }

            if (isInformationButtonVisible) {
                ActionButton(
                    iconDrawableRes = R.drawable.ic_info_filled,
                    contentDescriptionRes = R.string.tab_info,
                    onClick = { onInformationClicked?.invoke() },
                )
            }

            if (isDownloadButtonVisible) {
                ActionButton(
                    iconDrawableRes = R.drawable.ic_file_download_filled,
                    contentDescriptionRes = R.string.bottom_action_bar_download,
                    onClick = { onDownloadButtonClicked?.invoke() },
                    showProgress = isDownloading,
                )
            }

            if (isEditButtonVisible) {
                ActionButton(
                    iconDrawableRes = R.drawable.ic_edit_filled,
                    contentDescriptionRes = R.string.edit_live_wallpaper,
                    onClick = { editButtonIntent?.let { onNavigateToEditScreen.invoke(it) } },
                )
            }

            if (isCustomizeVisible) {
                ActionButton(
                    iconDrawableRes = R.drawable.ic_tune_filled,
                    contentDescriptionRes = R.string.tab_customize,
                    onClick = { onCustomizeClicked?.invoke() },
                )
            }

            if (isShareButtonVisible) {
                ActionButton(
                    iconDrawableRes = R.drawable.ic_share_filled,
                    contentDescriptionRes = R.string.tab_share,
                    onClick = { shareButtonIntent?.let { onStartShareActivity.invoke(it) } },
                )
            }

            if (isDeleteButtonVisible) {
                ActionButton(
                    iconDrawableRes = R.drawable.ic_delete_filled,
                    contentDescriptionRes = R.string.delete_live_wallpaper,
                    onClick = { onDeleteButtonClicked?.invoke() },
                )
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
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(modifier = Modifier.padding(vertical = 4.dp).padding(end = 16.dp)) {
            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = { backDispatcher?.onBackPressed() },
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

@Composable
private fun ActionButton(
    @DrawableRes iconDrawableRes: Int,
    @StringRes contentDescriptionRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
) {
    val colorScheme: ColorScheme = MaterialTheme.colorScheme

    IconButton(
        modifier = modifier.size(56.dp),
        onClick = { onClick.invoke() },
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = colorScheme.surfaceContainerHighest
            ),
        shape = CircleShape,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        } else {
            Icon(
                imageVector = ImageVector.vectorResource(iconDrawableRes),
                contentDescription = stringResource(contentDescriptionRes),
                tint = colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ActionButtonWithLabel(
    @DrawableRes iconDrawableRes: Int,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme: ColorScheme = MaterialTheme.colorScheme

    Button(
        modifier = modifier.height(56.dp),
        onClick = { onClick.invoke() },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceContainerHighest,
                contentColor = colorScheme.primary,
            ),
        shape = RoundedCornerShape(percent = 50),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconDrawableRes),
                contentDescription = null, // Handled by the button label
            )
            Text(text = stringResource(labelRes))
        }
    }
}
