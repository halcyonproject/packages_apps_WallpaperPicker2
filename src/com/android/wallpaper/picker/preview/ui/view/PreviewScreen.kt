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

import android.graphics.Bitmap
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.PreviewTarget

/** The screen that hosts the lock/home screen preview */
@Composable
fun PreviewScreen(
    preview: View,
    viewModel: WallpaperPreviewViewModel,
    previewTarget: PreviewTarget,
    modifier: Modifier,
) {
    // TODO (b/465178380): Use the real corner radius according to DeviceDisplayType
    Box(modifier = modifier.clip(RoundedCornerShape(percent = 10))) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { preview })

        PreviewShade(
            viewModel = viewModel,
            previewTarget = previewTarget,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun PreviewShade(
    viewModel: WallpaperPreviewViewModel,
    previewTarget: PreviewTarget,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    val lowResBitmap: Bitmap? by
        viewModel.staticWallpaperPreviewViewModel.lowResBitmap.collectAsStateWithLifecycle(null)
    val shadeAlpha: Float by
        viewModel.previewShadeAlpha(previewTarget).collectAsStateWithLifecycle()
    val shadeAnimateAlpha: Float by animateFloatAsState(shadeAlpha)

    Box(modifier = modifier.alpha(shadeAnimateAlpha).background(colorScheme.surfaceContainer)) {
        lowResBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier =
                    Modifier.fillMaxSize()
                        .blur(radius = 50.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
