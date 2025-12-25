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

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.slice.widget.SliceLiveData
import androidx.slice.widget.SliceView
import com.android.compose.modifiers.padding
import com.android.wallpaper.model.WallpaperAction
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.CreativeEffectFloatingSheetViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.PreviewFloatingSheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The bottom sheet is used on the wallpaper preview screen to show contents corresponding to clicks
 * on different action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewBottomSheet(
    previewFloatingSheetViewModel: PreviewFloatingSheetViewModel,
    logger: UserEventLogger,
    onBottomSheetCollapsed: () -> Unit,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        onDismissRequest = {
            coroutineScope
                .launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onBottomSheetCollapsed.invoke()
                    }
                }
        },
        sheetState = sheetState,
    ) {
        val (
            informationViewModel,
            imageEffectViewModel,
            creativeEffectViewModel,
            customizeViewModel,
        ) = previewFloatingSheetViewModel

        if (informationViewModel != null) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
                PreviewBottomSheetInformationContent(
                    informationViewModel = informationViewModel,
                    logger = logger,
                )
            }
        } else if (imageEffectViewModel != null) {
            // Do nothing intended. Image effect bottom sheet is not in use.
        } else if (creativeEffectViewModel != null) {
            CreativeEffectContent(viewModel = creativeEffectViewModel)
        } else if (customizeViewModel != null) {
            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                ComposeSliceView(customizeViewModel.customizeSliceUri)
            }
        }
    }
}

@Composable
fun CreativeEffectContent(
    viewModel: CreativeEffectFloatingSheetViewModel,
    modifier: Modifier = Modifier,
) {
    // Only 1 wallpaper action is supported
    val switchItemIndex = 0
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    val title: String = viewModel.title
    val subtitle: String = viewModel.subtitle

    val action: WallpaperAction? = viewModel.wallpaperActions.getOrNull(switchItemIndex)
    var isChecked: Boolean by remember { mutableStateOf(action?.toggled ?: false) }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        action?.let {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable {
                            isChecked = !isChecked
                            coroutineScope.launch {
                                // If checked, the checked item index is 0. -1 means unchecking all.
                                viewModel.wallpaperEffectSwitchListener.invoke(
                                    if (isChecked) switchItemIndex else -1
                                )
                            }
                        }
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                action.label?.let {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }

                Switch(
                    checked = isChecked,
                    onCheckedChange = null, // null because the Row's clickable handles it
                )
            }
        }
    }
}

@Composable
private fun ComposeSliceView(sliceUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val sliceView = remember {
        SliceView(context).apply {
            mode = SliceView.MODE_LARGE
            isScrollable = false
        }
    }

    DisposableEffect(sliceUri) {
        val liveData = SliceLiveData.fromUri(context, sliceUri)
        liveData.observeForever(sliceView)
        onDispose { liveData.removeObserver(sliceView) }
    }

    AndroidView(
        factory = { sliceView },
        modifier = modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 24.dp),
    )
}
