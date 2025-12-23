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

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.app.wallpaper.WallpaperDescription
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.RemoteException
import android.service.wallpaper.IWallpaperEngine
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.SurfaceControl
import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination.Companion.toSetWallpaperFlags
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.preview.shared.model.CropSizeModel
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.ui.binder.StaticWallpaperPreviewBinder2
import com.android.wallpaper.picker.preview.ui.util.SubsamplingScaleImageViewUtil.setOnNewCropListener
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.Companion.isCroppable
import com.android.wallpaper.picker.preview.ui.viewmodel.WorkspacePreviewConfigViewModel
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import com.android.wallpaper.util.WallpaperCropUtils
import com.android.wallpaper.util.wallpaperconnection.LiveWallpaperConnectionUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.lang.Integer.min
import kotlin.coroutines.resume
import kotlin.math.max
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
        windowToken: IBinder,
        liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils,
    ) {
        val wallpaperSurfaceControl: MutableStateFlow<WallpaperSurfaceControl?> =
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
                        val (wallpaper, whichPreview) = smallWallpaper

                        if (wallpaper is LiveWallpaperModel) {
                            // TODO (b/423956081): Figure out when we need to force single engine.
                            //                     We may use wallpaper.shouldEnforceSingleEngine()
                            val forceSingleEngine = true

                            val liveWallpaperSurfaceControl: WallpaperSurfaceControl.Live? =
                                renderLiveWallpaperPreview(
                                    context = applicationContext,
                                    wallpaperModel = wallpaper,
                                    forceSingleEngine = forceSingleEngine,
                                    displaySize = displaySize,
                                    whichPreview = whichPreview,
                                    windowToken = windowToken,
                                    destinationFlag = screen.toFlag(),
                                    liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                                    onEngineCreated = { engine ->
                                        // Note that there will be only one engine created callback
                                        // when forceSingleEngine is true
                                        preview.viewTreeObserver
                                            .addOnWindowVisibilityChangeListener { visibility ->
                                                engine.trySetIsVisible(visibility == View.VISIBLE)
                                            }
                                        // TODO (b/423956081): Figure out when we should pass the
                                        //                     touch event.
                                        // Set up on dispatch touch event
                                        if (forceSingleEngine) {
                                            Screen.entries.forEach {
                                                setOnDispatchTouchEventForLiveWallpapers(
                                                    viewModel = viewModel,
                                                    screen = it,
                                                    engine = engine,
                                                )
                                            }
                                        } else {
                                            setOnDispatchTouchEventForLiveWallpapers(
                                                viewModel = viewModel,
                                                screen = screen,
                                                engine = engine,
                                            )
                                        }
                                        // Set up on apply live wallpaper callback
                                        setOnApplyLiveWallpaper(
                                            viewModel = viewModel,
                                            engine = engine,
                                        )
                                    },
                                    onWallpaperColorsChanged = { colors, displayId, persistedColors
                                        ->
                                        // TODO(b/423956081): Handle color updates.
                                    },
                                )
                            // Release before assigning a new surface control
                            wallpaperSurfaceControl.value?.release()
                            wallpaperSurfaceControl.value = liveWallpaperSurfaceControl
                        } else if (wallpaper is WallpaperModel.StaticWallpaperModel) {
                            val staticWallpaperSurfaceControl =
                                renderStaticWallpaperPreview(
                                    applicationContext = applicationContext,
                                    lifecycleOwner = viewLifecycleOwner,
                                    screen = screen,
                                    viewModel = viewModel,
                                    displaySize = displaySize,
                                    display = display,
                                    hostToken = hostToken,
                                )
                            // Release before assigning a new surface control view host
                            wallpaperSurfaceControl.value?.release()
                            wallpaperSurfaceControl.value = staticWallpaperSurfaceControl
                        }
                    }
                }

                // TODO(b/423956081): Listen to preferredClockSize and enforce lock screen workspace
                //   preview to show correspondent clock.
                launch {
                    viewModel.wallpaperColorsModel.collect { wallpaperColorsModel ->
                        if (workspaceSurfaceControl.value == null) {
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
                            workspaceSurfaceControl.value = workspaceRenderResult?.surfaceControl
                        } else {
                            // TODO (b/423956081): Use callback messages to update the color.
                        }
                    }
                }

                launch {
                    combine(
                            wallpaperSurfaceControl.filterNotNull(),
                            workspaceSurfaceControl.filterNotNull(),
                            ::Pair,
                        )
                        .collect { (wallpaperSurfaceControl, workspaceSurfaceControl) ->
                            surfaceViewCallback?.let { preview.holder.removeCallback(it) }
                            preview.holder.addCallback(
                                object : SurfaceViewUtils.SurfaceCallback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            preview.reparentWallpaper(wallpaperSurfaceControl)
                                            preview.reparentWorkspace(workspaceSurfaceControl)
                                            viewModel.setPreviewReady(
                                                screen = screen,
                                                isReady = true,
                                            )
                                        }
                                    }
                                    .also { surfaceViewCallback = it }
                            )

                            // This is a workaround to force trigger a surfaceCreated from the
                            // SurfaceView, where the reparent transactions can work the most
                            // reliably.
                            val parentView: ViewGroup? = preview.parent as? ViewGroup
                            parentView?.let {
                                it.removeView(preview)
                                it.addView(preview)
                            }
                        }
                }
            }
            // Release when destroyed
            wallpaperSurfaceControl.value?.release()
            wallpaperSurfaceControl.value = null
            workspaceSurfaceControl.value?.release()
            workspaceSurfaceControl.value = null
            surfaceViewCallback?.let { preview.holder.removeCallback(it) }
            surfaceViewCallback = null
        }
    }

    private suspend fun renderLiveWallpaperPreview(
        context: Context,
        wallpaperModel: LiveWallpaperModel,
        forceSingleEngine: Boolean,
        displaySize: Point,
        whichPreview: WhichPreview,
        windowToken: IBinder,
        destinationFlag: Int,
        liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils,
        onEngineCreated: (engine: IWallpaperEngine) -> Unit,
        onWallpaperColorsChanged:
            (colors: WallpaperColors?, displayId: Int, persistedColors: WallpaperColors?) -> Unit,
    ): WallpaperSurfaceControl.Live? {
        val engine =
            liveWallpaperConnectionUtils.connect(
                context = context,
                wallpaperModel = wallpaperModel,
                forceSingleEngine = forceSingleEngine,
                destinationFlag = destinationFlag,
                displaySize = displaySize,
                windowToken = windowToken,
                displayId = 0, // TODO(b/423956081): give a proper ID here
                whichPreview = whichPreview,
                onEngineCreated = onEngineCreated,
                onWallpaperColorsChanged = onWallpaperColorsChanged,
            )
        return engine.mirrorSurfaceControl()?.let { surfaceControl ->
            WallpaperSurfaceControl.Live(surfaceControl, engine)
        }
    }

    private fun renderStaticWallpaperPreview(
        applicationContext: Context,
        lifecycleOwner: LifecycleOwner,
        screen: Screen,
        viewModel: WallpaperPreviewViewModel,
        displaySize: Point,
        display: Display,
        hostToken: IBinder,
    ): WallpaperSurfaceControl.Static {
        val scaleImageView =
            SubsamplingScaleImageView(applicationContext).apply {
                setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
                setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)

                if (viewModel.wallpaper.value?.isCroppable() == true) {
                    initCrop(
                        applicationContext = applicationContext,
                        viewModel = viewModel,
                        displaySize = displaySize,
                    )
                    setOnDispatchTouchEvent(viewModel = viewModel, screen = screen)
                }
            }
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
        return WallpaperSurfaceControl.Static(surfaceControlViewHost)
    }

    private fun SubsamplingScaleImageView.initCrop(
        applicationContext: Context,
        viewModel: WallpaperPreviewViewModel,
        displaySize: Point,
    ) {
        this.doOnLayout {
            val imageSize = Point(this.width, this.height)
            val cropImageSize =
                WallpaperCropUtils.calculateCropSurfaceSize(
                    applicationContext.resources,
                    max(imageSize.x, imageSize.y),
                    min(imageSize.x, imageSize.y),
                    imageSize.x,
                    imageSize.y,
                )

            this.setOnNewCropListener { crop, zoom ->
                viewModel.staticWallpaperPreviewViewModel.fullPreviewCropModels[displaySize] =
                    FullPreviewCropModel(
                        cropHint = crop,
                        cropSizeModel =
                            CropSizeModel(
                                wallpaperZoom = zoom,
                                hostViewSize = imageSize,
                                cropViewSize = cropImageSize,
                            ),
                    )
            }
        }
    }

    private fun View.setOnDispatchTouchEvent(viewModel: WallpaperPreviewViewModel, screen: Screen) {
        when (screen) {
            Screen.LOCK_SCREEN -> {
                viewModel.setOnLockDispatchTouchEvent { event: MotionEvent ->
                    this.dispatchTouchEvent(event)
                }
            }
            Screen.HOME_SCREEN -> {
                viewModel.setOnHomeDispatchTouchEvent { event: MotionEvent ->
                    this.dispatchTouchEvent(event)
                }
            }
        }
    }

    private fun setOnDispatchTouchEventForLiveWallpapers(
        viewModel: WallpaperPreviewViewModel,
        screen: Screen,
        engine: IWallpaperEngine,
    ) {
        val onDispatchTouchEvent: (event: MotionEvent) -> Unit = { event: MotionEvent ->
            val action: Int = event.actionMasked
            val dup = MotionEvent.obtainNoHistory(event).also { it.setLocation(event.x, event.y) }
            val pointerIndex = event.actionIndex
            try {
                engine.dispatchPointer(dup)
                if (action == MotionEvent.ACTION_UP) {
                    engine.dispatchWallpaperCommand(
                        WallpaperManager.COMMAND_TAP,
                        event.x.toInt(),
                        event.y.toInt(),
                        0,
                        null,
                    )
                } else if (action == MotionEvent.ACTION_POINTER_UP) {
                    engine.dispatchWallpaperCommand(
                        WallpaperManager.COMMAND_SECONDARY_TAP,
                        event.getX(pointerIndex).toInt(),
                        event.getY(pointerIndex).toInt(),
                        0,
                        null,
                    )
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Remote exception of wallpaper connection", e)
            }
        }
        when (screen) {
            Screen.LOCK_SCREEN -> {
                viewModel.setOnLockDispatchTouchEvent(onDispatchTouchEvent)
            }
            Screen.HOME_SCREEN -> {
                viewModel.setOnHomeDispatchTouchEvent(onDispatchTouchEvent)
            }
        }
    }

    // This is an essential callback to the live wallpaper engine to update the WallpaperDescription
    // before setting live wallpapers to the system.
    private fun setOnApplyLiveWallpaper(
        viewModel: WallpaperPreviewViewModel,
        engine: IWallpaperEngine,
    ) {
        viewModel.setOnApplyLiveWallpaper { destination ->
            try {
                (engine.javaClass
                    .getMethod("onApplyWallpaper", Int::class.javaPrimitiveType)
                    .invoke(engine, destination.toSetWallpaperFlags()) as WallpaperDescription?)
            } catch (e: RemoteException) {
                // We catch this explicitly because it means that the method is defined, but the
                // bound object is dead.
                Log.w(TAG, "Error calling onApplyWallpaper", e)
                null
            }
        }
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

    private fun SurfaceView.reparentWallpaper(wallpaperSurfaceControl: WallpaperSurfaceControl) {
        when (wallpaperSurfaceControl) {
            is WallpaperSurfaceControl.Live -> {
                reparentLiveWallpaper(wallpaperSurfaceControl.liveWallpaperSurfaceControl)
            }
            is WallpaperSurfaceControl.Static -> {
                reparentStaticWallpaper(
                    wallpaperSurfaceControl.staticWallpaperSurfaceControlViewHost
                )
            }
        }
    }

    private fun SurfaceView.reparentLiveWallpaper(surfaceControl: SurfaceControl) {
        val surfaceViewRootSurfaceControl = this.rootSurfaceControl
        if (surfaceViewRootSurfaceControl == null) {
            Log.w(
                TAG,
                "Unable to reparent workspace SurfaceControl to $this, since SurfaceView's " +
                    "rootSurfaceControl is null.",
            )
            return
        }
        val transaction =
            SurfaceControl.Transaction()
                .reparent(surfaceControl, this.surfaceControl)
                .setLayer(surfaceControl, 0)
                .show(surfaceControl)
        surfaceViewRootSurfaceControl.applyTransactionOnDraw(transaction)
    }

    private fun SurfaceView.reparentStaticWallpaper(
        surfaceControlViewHost: SurfaceControlViewHost
    ) {
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

    private fun IWallpaperEngine.trySetIsVisible(isVisible: Boolean) {
        try {
            setVisibility(isVisible)
        } catch (e: RemoteException) {
            Log.w(TAG, "Error setting engine visibility", e)
        }
    }

    private data class WorkspaceRenderResult(
        val surfaceControl: SurfaceControl,
        val callback: Message,
    )

    sealed class WallpaperSurfaceControl {
        abstract fun release()

        /**
         * @param engine We need to keep the reference of the engine to set visibility when surface
         *   is created and destroyed.
         */
        data class Live(
            val liveWallpaperSurfaceControl: SurfaceControl,
            val engine: IWallpaperEngine,
        ) : WallpaperSurfaceControl() {
            override fun release() {
                liveWallpaperSurfaceControl.release()
            }
        }

        data class Static(val staticWallpaperSurfaceControlViewHost: SurfaceControlViewHost) :
            WallpaperSurfaceControl() {
            override fun release() {
                staticWallpaperSurfaceControlViewHost.release()
            }
        }
    }

    private const val TAG = "PreviewBinder"
}
