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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.Display
import android.view.SurfaceControl
import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.ui.binder.StaticWallpaperPreviewBinder2
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WorkspacePreviewConfigViewModel
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Binder for the lock/home screen preview. */
object PreviewBinder {

    fun bind(
        preview: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        screen: Screen,
        displaySize: Point,
        deviceDisplayType: DeviceDisplayType,
        display: Display,
        hostToken: IBinder,
    ) {
        val staticWallpaperSurfaceControlViewHost: MutableStateFlow<SurfaceControlViewHost?> =
            MutableStateFlow(null)
        val workspaceSurfaceControl: MutableStateFlow<SurfaceControl?> = MutableStateFlow(null)
        var surfaceViewCallback: SurfaceViewUtils.SurfaceCallback? = null

        // Set fixed size as the size of the correspondent display, which is the largest size a
        // preview is expected to expand. So that we will not lose image resolution when expanding.
        preview.holder.setFixedSize(displaySize.x, displaySize.y)
        // Make surface view's lifecycle follows attach and detach, instead of visibility
        preview.setSurfaceLifecycle(SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.smallWallpaper.collect { smallWallpaper ->
                        val (wallpaper, _) = smallWallpaper

                        if (wallpaper is WallpaperModel.LiveWallpaperModel) {
                            // TODO(b/423956081): handle live wallpaper
                        } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                            val surfaceControlViewHost =
                                renderStaticWallpaperPreview(
                                    applicationContext = applicationContext,
                                    lifecycleOwner = viewLifecycleOwner,
                                    viewModel = viewModel,
                                    displaySize = displaySize,
                                    display = display,
                                    hostToken = hostToken,
                                )
                            // Release before assigning a new surface control view host
                            staticWallpaperSurfaceControlViewHost.value?.release()
                            staticWallpaperSurfaceControlViewHost.value = surfaceControlViewHost
                        }
                    }
                }

                // TODO(b/423956081): Listen to preferredClockSize and enforce lock screen workspace
                //   preview to show correspondent clock.
                launch {
                    viewModel.wallpaperColorsModel.collect { wallpaperColorsModel ->
                        // Create SurfaceControl for the workspace
                        val workspaceRenderResult: WorkspaceRenderResult? =
                            renderWorkspacePreview(
                                screen = screen,
                                deviceDisplayType = deviceDisplayType,
                                wallpaperPreviewViewModel = viewModel,
                                wallpaperColorsModel = wallpaperColorsModel,
                                displaySize = displaySize,
                                hostToken = hostToken,
                            )
                        // Release before assigning a new surface control
                        workspaceSurfaceControl.value?.release()
                        workspaceSurfaceControl.value = workspaceRenderResult?.surfaceControl
                    }
                }

                launch {
                    combine(
                            staticWallpaperSurfaceControlViewHost.filterNotNull(),
                            workspaceSurfaceControl.filterNotNull(),
                            ::Pair,
                        )
                        .collect { (staticWallpaperSurfaceControlViewHost, workspaceSurfaceControl)
                            ->
                            fun reparent() {
                                preview.reparentWallpaper(staticWallpaperSurfaceControlViewHost)
                                preview.reparentWorkspace(workspaceSurfaceControl)
                            }

                            surfaceViewCallback?.let { preview.holder.removeCallback(it) }
                            preview.holder.addCallback(
                                object : SurfaceViewUtils.SurfaceCallback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            reparent()
                                        }
                                    }
                                    .also { surfaceViewCallback = it }
                            )

                            reparent()
                        }
                }
            }
            // Release when destroyed
            staticWallpaperSurfaceControlViewHost.value?.release()
            staticWallpaperSurfaceControlViewHost.value = null
            workspaceSurfaceControl.value?.release()
            workspaceSurfaceControl.value = null
            surfaceViewCallback?.let { preview.holder.removeCallback(it) }
            surfaceViewCallback = null
        }
    }

    private fun renderStaticWallpaperPreview(
        applicationContext: Context,
        lifecycleOwner: LifecycleOwner,
        viewModel: WallpaperPreviewViewModel,
        displaySize: Point,
        display: Display,
        hostToken: IBinder,
    ): SurfaceControlViewHost {
        val scaleImageView = SubsamplingScaleImageView(applicationContext)
        val surfaceControlViewHost =
            SurfaceControlViewHost(applicationContext, display, hostToken).apply {
                setView(scaleImageView, displaySize.x, displaySize.y)
            }
        StaticWallpaperPreviewBinder2.bind(
            applicationContext = applicationContext,
            scaleImageView = scaleImageView,
            viewModel = viewModel.staticWallpaperPreviewViewModel,
            displaySize = displaySize,
            lifecycleOwner = lifecycleOwner,
        )
        // TODO(b/423956081): PreviewEffectsLoadingBinder to bind loading effect
        return surfaceControlViewHost
    }

    private suspend fun renderWorkspacePreview(
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        wallpaperColorsModel: WallpaperColorsModel,
        displaySize: Point,
        hostToken: IBinder,
    ): WorkspaceRenderResult? {
        var result: WorkspaceRenderResult? = null
        val workspacePreviewConfig: WorkspacePreviewConfigViewModel =
            wallpaperPreviewViewModel.getWorkspacePreviewConfig(
                screen = screen,
                deviceDisplayType = deviceDisplayType,
            )
        if (
            workspacePreviewConfig.previewUtils.supportsPreview() &&
                wallpaperColorsModel is WallpaperColorsModel.Loaded
        ) {
            val displayId: Int =
                wallpaperPreviewViewModel.getDisplayId(workspacePreviewConfig.deviceDisplayType)
            val extras =
                bundleOf(
                        SurfaceViewUtils.KEY_HOST_TOKEN to hostToken,
                        SurfaceViewUtils.KEY_DISPLAY_ID to displayId,
                        SurfaceViewUtils.KEY_VIEW_WIDTH to displaySize.x,
                        SurfaceViewUtils.KEY_VIEW_HEIGHT to displaySize.y,
                    )
                    .apply {
                        wallpaperColorsModel.colors?.let { colors ->
                            putParcelable(SurfaceViewUtils.KEY_WALLPAPER_COLORS, colors)
                        }
                    }
            result = suspendCancellableCoroutine { continuation ->
                workspacePreviewConfig.previewUtils.renderPreview(
                    extras,
                    object : PreviewUtils.WorkspacePreviewCallback {
                        override fun onPreviewRendered(resultBundle: Bundle?) {
                            val surfaceControl: SurfaceControl? =
                                resultBundle?.let {
                                    SurfaceViewUtils.getSurfacePackage(it)?.surfaceControl
                                }
                            val surfaceCallback: Message? =
                                resultBundle?.let { SurfaceViewUtils.getCallback(resultBundle) }
                            if (surfaceControl != null && surfaceCallback != null) {
                                continuation.resume(
                                    WorkspaceRenderResult(surfaceControl, surfaceCallback)
                                )
                            } else {
                                continuation.resume(null)
                            }
                        }
                    },
                )
            }
        }
        return result
    }

    private fun SurfaceView.reparentWallpaper(surfaceControlViewHost: SurfaceControlViewHost) {
        val surfacePackage = surfaceControlViewHost.surfacePackage
        if (surfacePackage != null) {
            this.setChildSurfacePackage(surfacePackage)
        } else {
            Log.w(
                TAG,
                "Unable to reparent$surfaceControlViewHost to $this since its " +
                    "SurfacePackage is null.",
            )
        }
    }

    private fun SurfaceView.reparentWorkspace(workspaceSurfaceControl: SurfaceControl) {
        val surfaceViewRootSurfaceControl = this.rootSurfaceControl
        if (surfaceViewRootSurfaceControl == null) {
            Log.w(
                TAG,
                "Unable to reparent $workspaceSurfaceControl to $this, since SurfaceView's " +
                    "rootSurfaceControl is null.",
            )
            return
        }
        val transaction =
            SurfaceControl.Transaction()
                .reparent(workspaceSurfaceControl, this.surfaceControl)
                .setLayer(workspaceSurfaceControl, 1)
                .show(workspaceSurfaceControl)
        surfaceViewRootSurfaceControl.applyTransactionOnDraw(transaction)
    }

    private data class WorkspaceRenderResult(
        val surfaceControl: SurfaceControl,
        val callback: Message,
    )

    private const val TAG = "PreviewBinder"
}
