/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.wallpaper.picker.preview.ui.util

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.android.wallpaper.util.WallpaperCropUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

object FullResImageViewUtil {

    private const val DEFAULT_WALLPAPER_MAX_ZOOM = 8f

    /**
     * Calculates minimum zoom to fit maximum visible area of wallpaper on crop surface.
     *
     * Preserves a boundary at [systemScale] beyond the visible crop when given.
     *
     * @param systemScale the device's system wallpaper scale when it needs to be considered
     */
    fun getScaleAndCenter(
        viewSize: Point,
        rawWallpaperSize: Point,
        displaySize: Point,
        cropRect: Rect?,
        isRtl: Boolean,
        systemScale: Float = 1f,
    ): ScaleAndCenter {
        viewSize.apply {
            // Preserve precision by not converting scale to int but the result
            x = (x * systemScale).toInt()
            y = (y * systemScale).toInt()
        }
        // defaultRawWallpaperRect represents a brand new wallpaper preview with no crop hints.
        val defaultRawWallpaperRect =
            WallpaperCropUtils.calculateVisibleRect(rawWallpaperSize, viewSize)
        val visibleRawWallpaperRect =
            cropRect?.let { CropSizeUtil.fitCropRectToLayoutDirection(it, displaySize, isRtl) }
                ?: defaultRawWallpaperRect

        val centerPosition =
            PointF(
                visibleRawWallpaperRect.centerX().toFloat(),
                visibleRawWallpaperRect.centerY().toFloat(),
            )
        val defaultWallpaperZoom =
            WallpaperCropUtils.calculateMinZoom(
                Point(defaultRawWallpaperRect.width(), defaultRawWallpaperRect.height()),
                viewSize,
            )
        val visibleWallpaperZoom =
            WallpaperCropUtils.calculateMinZoom(
                Point(visibleRawWallpaperRect.width(), visibleRawWallpaperRect.height()),
                viewSize,
            )

        return ScaleAndCenter(
            defaultWallpaperZoom,
            defaultWallpaperZoom.coerceAtLeast(DEFAULT_WALLPAPER_MAX_ZOOM),
            visibleWallpaperZoom,
            centerPosition,
        )
    }

    /**
     * Calculates the scale and center point for a wallpaper preview with floating-point precision.
     *
     * This is the high-precision counterpart to [getScaleAndCenter].
     */
    fun getScaleAndCenterF(
        rawWallpaperSize: Point,
        displaySize: Point,
        scaleImageViewSize: Point,
        cropRect: Rect?,
        isRtl: Boolean,
    ): ScaleAndCenter {
        // defaultRawWallpaperRect represents a brand new wallpaper preview with no crop hints.
        val defaultRawWallpaperRect: RectF =
            calculateVisibleRect(rawWallpaperSize, scaleImageViewSize)
        val visibleRawWallpaperRect: RectF =
            cropRect?.let { fitCropRectToLayoutDirection(it, displaySize, isRtl) }
                ?: defaultRawWallpaperRect

        val centerPosition =
            PointF(visibleRawWallpaperRect.centerX(), visibleRawWallpaperRect.centerY())
        val defaultWallpaperZoom =
            calculateMinZoom(
                PointF(defaultRawWallpaperRect.width(), defaultRawWallpaperRect.height()),
                PointF(scaleImageViewSize.x.toFloat(), scaleImageViewSize.y.toFloat()),
            )
        val visibleWallpaperZoom =
            calculateMinZoom(
                PointF(visibleRawWallpaperRect.width(), visibleRawWallpaperRect.height()),
                PointF(scaleImageViewSize.x.toFloat(), scaleImageViewSize.y.toFloat()),
            )

        return ScaleAndCenter(
            defaultWallpaperZoom,
            defaultWallpaperZoom.coerceAtLeast(DEFAULT_WALLPAPER_MAX_ZOOM),
            visibleWallpaperZoom,
            centerPosition,
        )
    }

    private fun calculateVisibleRect(outer: Point, inner: Point): RectF {
        val visibleRectCenter = PointF(outer.x / 2f, outer.y / 2f)
        if (inner.x / inner.y.toFloat() > outer.x / outer.y.toFloat()) {
            val minZoom = inner.x / outer.x.toFloat()
            val visibleRectHeight = inner.y / minZoom
            return RectF(
                0f,
                visibleRectCenter.y - visibleRectHeight / 2,
                outer.x.toFloat(),
                visibleRectCenter.y + visibleRectHeight / 2,
            )
        } else {
            val minZoom = inner.y / outer.y.toFloat()
            val visibleRectWidth = inner.x / minZoom
            return RectF(
                visibleRectCenter.x - visibleRectWidth / 2,
                0f,
                visibleRectCenter.x + visibleRectWidth / 2,
                outer.y.toFloat(),
            )
        }
    }

    private fun fitCropRectToLayoutDirection(
        cropRect: Rect,
        displaySize: Point,
        isRtl: Boolean,
    ): RectF {
        val parallax = cropRect.width() - displaySize.x * cropRect.height() / displaySize.y
        return RectF(cropRect).apply { if (isRtl) left += parallax else right -= parallax }
    }

    private fun calculateMinZoom(outer: PointF, inner: PointF): Float {
        val minZoom =
            if (inner.x / inner.y > outer.x / outer.y) {
                inner.x / outer.x
            } else {
                inner.y / outer.y
            }
        return minZoom
    }

    fun SubsamplingScaleImageView.getCropRect() = Rect().apply { visibleFileRect(this) }

    data class ScaleAndCenter(
        val minScale: Float,
        val maxScale: Float,
        val defaultScale: Float,
        val center: PointF,
    )
}
