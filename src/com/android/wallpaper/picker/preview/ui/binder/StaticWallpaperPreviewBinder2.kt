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
package com.android.wallpaper.picker.preview.ui.binder

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.app.tracing.TraceUtils.trace
import com.android.wallpaper.picker.preview.shared.model.CropSizeModel
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.StaticWallpaperPreviewViewModel
import com.android.wallpaper.util.RtlUtils
import com.android.wallpaper.util.WallpaperCropUtils
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

object StaticWallpaperPreviewBinder2 {

    fun bind(
        applicationContext: Context,
        scaleImageView: SubsamplingScaleImageView,
        viewModel: StaticWallpaperPreviewViewModel,
        displaySize: Point,
        scaleImageViewSize: Point,
        lifecycleOwner: LifecycleOwner,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.subsamplingScaleImageViewModel.collect { imageModel ->
                    trace(TAG) {
                        val cropHint: Rect? =
                            imageModel.fullPreviewCropModels?.get(displaySize)?.cropHint
                        scaleImageView.setImage(
                            imageSource = ImageSource.cachedBitmap(imageModel.rawWallpaperBitmap),
                            rawWallpaperSize = imageModel.rawWallpaperSize,
                            displaySize = displaySize,
                            scaleImageViewSize = scaleImageViewSize,
                            cropHint = cropHint,
                            isRtl = RtlUtils.isRtl(applicationContext),
                        )
                        // Initialize the preview crop, so that we can extract the color from the
                        // wallpaper. The color is essential to render the workspace preview.
                        viewModel.updateDefaultPreviewCropModel(
                            displaySize,
                            FullPreviewCropModel(
                                cropHint =
                                    WallpaperCropUtils.calculateVisibleRect(
                                        imageModel.rawWallpaperSize,
                                        scaleImageViewSize,
                                    ),
                                cropSizeModel =
                                    CropSizeModel(
                                        wallpaperZoom =
                                            WallpaperCropUtils.calculateMinZoom(
                                                imageModel.rawWallpaperSize,
                                                scaleImageViewSize,
                                            ),
                                        hostViewSize = scaleImageViewSize,
                                        cropViewSize =
                                            WallpaperCropUtils.calculateCropSurfaceSize(
                                                scaleImageView.resources,
                                                max(scaleImageViewSize.x, scaleImageViewSize.y),
                                                min(scaleImageViewSize.x, scaleImageViewSize.y),
                                                scaleImageViewSize.x,
                                                scaleImageViewSize.y,
                                            ),
                                    ),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun SubsamplingScaleImageView.setImage(
        imageSource: ImageSource,
        rawWallpaperSize: Point,
        displaySize: Point,
        scaleImageViewSize: Point,
        cropHint: Rect?,
        isRtl: Boolean,
    ) {
        // Set the full res image
        setImage(imageSource)
        // Calculate the scale and the center point for the full res image
        doOnLayout {
            FullResImageViewUtil.getScaleAndCenterF(
                    rawWallpaperSize = rawWallpaperSize,
                    displaySize = displaySize,
                    scaleImageViewSize = scaleImageViewSize,
                    cropRect = cropHint,
                    isRtl = isRtl,
                )
                .let { scaleAndCenter ->
                    minScale = scaleAndCenter.minScale
                    maxScale = scaleAndCenter.maxScale
                    setScaleAndCenter(scaleAndCenter.defaultScale, scaleAndCenter.center)
                }
        }
    }

    private const val TAG = "StaticWallpaperPreviewBinder2"
}
