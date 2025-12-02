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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.wallpaper.module.logging.UserEventLogger
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
            // TODO(b/465178376): Handle the case of imageEffectViewModel
        } else if (creativeEffectViewModel != null) {
            // TODO(b/465178376): Handle the case of creativeEffectViewModel.
        } else if (customizeViewModel != null) {
            // TODO(b/465178376): Handle the case of customizeViewModel
        }
    }
}
