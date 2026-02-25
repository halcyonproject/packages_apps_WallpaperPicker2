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
import android.graphics.Matrix
import android.graphics.Point
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.RemoteException
import android.service.wallpaper.IWallpaperEngine
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
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
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.FOLDED
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.SINGLE
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.UNFOLDED
import com.android.wallpaper.picker.common.preview.ui.binder.DefaultWorkspaceCallbackBinder.Companion.MESSAGE_ID_DESTROY_PREVIEW
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder.Companion.sendMessage
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
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.DisplaySizes
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.PreviewTarget
import com.android.wallpaper.picker.preview.ui.viewmodel.WorkspacePreviewConfigViewModel
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import com.android.wallpaper.util.WallpaperCropUtils
import com.android.wallpaper.util.wallpaperconnection.LiveWallpaperConnection
import com.android.wallpaper.util.wallpaperconnection.LiveWallpaperConnectionUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.Companion.shouldEnforceSingleEngine
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.lang.Integer.min
import kotlin.coroutines.resume
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Binder for the lock/home screen preview. */
object PreviewBinder {

    interface PreviewBinding {
        fun releasePreview()
    }

    fun bindSinglePreviews(
        applicationContext: Context,
        viewModel: WallpaperPreviewViewModel,
        viewLifecycleOwner: LifecycleOwner,
        liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils,
        display: Display,
        hostToken: IBinder,
        windowToken: IBinder,
        lockScreenPreview: SurfaceView,
        homeScreenPreview: SurfaceView,
        displaySizes: DisplaySizes,
        onDispatchTouchEventReady:
            (
                previewTarget: PreviewTarget, onDispatchTouchEvent: (event: MotionEvent) -> Unit,
            ) -> Unit,
    ): List<PreviewBinding> {
        return listOf(
            // Bind lock screen preview
            bind(
                preview = lockScreenPreview,
                viewModel = viewModel,
                applicationContext = applicationContext,
                viewLifecycleOwner = viewLifecycleOwner,
                previewTarget = PreviewTarget(LOCK_SCREEN, SINGLE),
                displaySizes = displaySizes,
                display = display,
                hostToken = hostToken,
                windowToken = windowToken,
                liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                onDispatchTouchEventReady = { onDispatchTouchEvent ->
                    onDispatchTouchEventReady.invoke(
                        PreviewTarget(LOCK_SCREEN, SINGLE),
                        onDispatchTouchEvent,
                    )
                },
            ),
            // Bind home screen preview
            bind(
                preview = homeScreenPreview,
                viewModel = viewModel,
                applicationContext = applicationContext,
                viewLifecycleOwner = viewLifecycleOwner,
                previewTarget = PreviewTarget(HOME_SCREEN, SINGLE),
                displaySizes = displaySizes,
                display = display,
                hostToken = hostToken,
                windowToken = windowToken,
                liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                onDispatchTouchEventReady = { onDispatchTouchEvent ->
                    onDispatchTouchEventReady.invoke(
                        PreviewTarget(HOME_SCREEN, SINGLE),
                        onDispatchTouchEvent,
                    )
                },
            ),
        )
    }

    fun bindFoldablePreviews(
        applicationContext: Context,
        viewModel: WallpaperPreviewViewModel,
        viewLifecycleOwner: LifecycleOwner,
        liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils,
        display: Display,
        hostToken: IBinder,
        windowToken: IBinder,
        lockScreenPreview: SurfaceView,
        lockScreenUnfoldedPreview: SurfaceView,
        homeScreenPreview: SurfaceView,
        homeScreenUnfoldedPreview: SurfaceView,
        displaySizes: DisplaySizes,
        onDispatchTouchEventReady:
            (
                previewTarget: PreviewTarget, onDispatchTouchEvent: (event: MotionEvent) -> Unit,
            ) -> Unit,
    ): List<PreviewBinding> {
        return listOf(
            // Bind lock screen previews
            bind(
                preview = lockScreenPreview,
                viewModel = viewModel,
                applicationContext = applicationContext,
                viewLifecycleOwner = viewLifecycleOwner,
                previewTarget = PreviewTarget(LOCK_SCREEN, FOLDED),
                displaySizes = displaySizes,
                display = display,
                hostToken = hostToken,
                windowToken = windowToken,
                liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                onDispatchTouchEventReady = { onDispatchTouchEvent ->
                    onDispatchTouchEventReady.invoke(
                        PreviewTarget(LOCK_SCREEN, FOLDED),
                        onDispatchTouchEvent,
                    )
                },
            ),
            bind(
                preview = lockScreenUnfoldedPreview,
                viewModel = viewModel,
                applicationContext = applicationContext,
                viewLifecycleOwner = viewLifecycleOwner,
                previewTarget = PreviewTarget(LOCK_SCREEN, UNFOLDED),
                displaySizes = displaySizes,
                display = display,
                hostToken = hostToken,
                windowToken = windowToken,
                liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                onDispatchTouchEventReady = { onDispatchTouchEvent ->
                    onDispatchTouchEventReady.invoke(
                        PreviewTarget(LOCK_SCREEN, UNFOLDED),
                        onDispatchTouchEvent,
                    )
                },
            ),
            // Bind home screen previews
            bind(
                preview = homeScreenPreview,
                viewModel = viewModel,
                applicationContext = applicationContext,
                viewLifecycleOwner = viewLifecycleOwner,
                previewTarget = PreviewTarget(HOME_SCREEN, FOLDED),
                displaySizes = displaySizes,
                display = display,
                hostToken = hostToken,
                windowToken = windowToken,
                liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                onDispatchTouchEventReady = { onDispatchTouchEvent ->
                    onDispatchTouchEventReady.invoke(
                        PreviewTarget(HOME_SCREEN, FOLDED),
                        onDispatchTouchEvent,
                    )
                },
            ),
            bind(
                preview = homeScreenUnfoldedPreview,
                viewModel = viewModel,
                applicationContext = applicationContext,
                viewLifecycleOwner = viewLifecycleOwner,
                previewTarget = PreviewTarget(HOME_SCREEN, UNFOLDED),
                displaySizes = displaySizes,
                display = display,
                hostToken = hostToken,
                windowToken = windowToken,
                liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                onDispatchTouchEventReady = { onDispatchTouchEvent ->
                    onDispatchTouchEventReady.invoke(
                        PreviewTarget(HOME_SCREEN, UNFOLDED),
                        onDispatchTouchEvent,
                    )
                },
            ),
        )
    }

    fun bind(
        preview: SurfaceView,
        viewModel: WallpaperPreviewViewModel,
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        previewTarget: PreviewTarget,
        displaySizes: DisplaySizes,
        display: Display,
        hostToken: IBinder,
        windowToken: IBinder,
        liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils,
        onDispatchTouchEventReady: (onDispatchTouchEvent: (event: MotionEvent) -> Unit) -> Unit,
        wallpaperPreviewOnly: Boolean = false,
    ): PreviewBinding {
        var workspaceCallback: Message? = null
        val wallpaperSurfaceControl: MutableStateFlow<WallpaperSurfaceControl?> =
            MutableStateFlow(null)
        val workspaceSurfaceControl: MutableStateFlow<SurfaceControl?> = MutableStateFlow(null)
        var surfaceViewCallback: SurfaceViewUtils.SurfaceCallback? = null

        fun cleanupWorkspacePreview() {
            workspaceCallback?.sendMessage(MESSAGE_ID_DESTROY_PREVIEW, Bundle())
            workspaceCallback = null
            workspaceSurfaceControl.value?.release()
            workspaceSurfaceControl.value = null
        }

        fun releasePreview() {
            cleanupWorkspacePreview()
            wallpaperSurfaceControl.value?.release()
            wallpaperSurfaceControl.value = null
            surfaceViewCallback?.let { preview.holder.removeCallback(it) }
            surfaceViewCallback = null
        }

        // Set fixed size as the size of the correspondent display, which is the largest size a
        // preview is expected to expand. So that we will not lose image resolution when expanding.
        val displaySize: Point = displaySizes.getDisplaySize(previewTarget.deviceDisplayType)
        preview.holder.setFixedSize(displaySize.x, displaySize.y)
        // Make surface view's lifecycle follows attach and detach, instead of visibility
        preview.setSurfaceLifecycle(SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.smallWallpaper.collect { smallWallpaper ->
                        val (wallpaper, whichPreview) = smallWallpaper

                        if (wallpaper is LiveWallpaperModel) {
                            val forceSingleEngine = wallpaper.shouldEnforceSingleEngine()
                            val liveWallpaperSurfaceControl: WallpaperSurfaceControl.Live? =
                                renderLiveWallpaperPreview(
                                    context = applicationContext,
                                    wallpaperModel = wallpaper,
                                    forceSingleEngine = forceSingleEngine,
                                    displaySize = displaySize,
                                    engineDisplaySize =
                                        displaySizes.getEngineDisplaySize(
                                            displayType = previewTarget.deviceDisplayType,
                                            forceSingleEngine = forceSingleEngine,
                                        ),
                                    whichPreview = whichPreview,
                                    windowToken = windowToken,
                                    destinationFlag = previewTarget.screen.toFlag(),
                                    liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                                    onEngineReady = { engine ->
                                        // Note that there will be only one engine created callback
                                        // when forceSingleEngine is true
                                        preview.viewTreeObserver
                                            .addOnWindowVisibilityChangeListener { visibility ->
                                                engine.trySetIsVisible(visibility == View.VISIBLE)
                                            }
                                        if (!wallpaperPreviewOnly) {
                                            // Set up on dispatch touch event
                                            onDispatchTouchEventReady.invoke(
                                                getOnDispatchTouchEventForLiveWallpapers(engine)
                                            )
                                            // Set up on apply live wallpaper callback
                                            setOnApplyLiveWallpaper(
                                                viewModel = viewModel,
                                                engine = engine,
                                            )
                                        }
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
                                    viewModel = viewModel,
                                    displaySize = displaySize,
                                    display = display,
                                    hostToken = hostToken,
                                    onDispatchTouchEventReady = onDispatchTouchEventReady,
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
                    viewModel.wallpaperColorsModel.collectLatest { wallpaperColorsModel ->
                        // TODO(b/423956081): To improve preview update quality, we should send
                        //  messages to workspaceCallback to update colors instead of recreating
                        //  a workspace render.
                        cleanupWorkspacePreview()
                        // TODO(b/485520081): Cancel the rendering of the preview when the coroutine
                        //  job is cancelled.
                        val workspaceRenderResult: WorkspaceRenderResult? =
                            renderWorkspacePreview(
                                previewTarget = previewTarget,
                                wallpaperPreviewViewModel = viewModel,
                                wallpaperColorsModel = wallpaperColorsModel,
                                displaySize = displaySize,
                                hostToken = hostToken,
                            )
                        workspaceCallback = workspaceRenderResult?.callback
                        workspaceSurfaceControl.value = workspaceRenderResult?.surfaceControl
                    }
                }

                launch {
                    combine(
                            wallpaperSurfaceControl.filterNotNull(),
                            if (wallpaperPreviewOnly) flowOf(null)
                            else workspaceSurfaceControl.filterNotNull(),
                            ::Pair,
                        )
                        .collect { (wallpaperSurfaceControl, workspaceSurfaceControl) ->
                            surfaceViewCallback?.let { preview.holder.removeCallback(it) }
                            preview.holder.addCallback(
                                object : SurfaceViewUtils.SurfaceCallback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            preview.reparentWallpaper(wallpaperSurfaceControl)
                                            if (workspaceSurfaceControl != null) {
                                                preview.reparentWorkspace(workspaceSurfaceControl)
                                            }
                                            viewModel.setPreviewReady2(
                                                previewTarget = previewTarget,
                                                isReady = true,
                                            )
                                        }
                                    }
                                    .also { surfaceViewCallback = it }
                            )
                            if (!preview.isVisible) {
                                // When SurfaceView turns from not visible to visible,
                                // it will trigger surfaceCreated.
                                preview.isVisible = true
                            } else {
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
            }
            // Release when destroyed
            releasePreview()
        }
        return object : PreviewBinding {
            override fun releasePreview() {
                releasePreview()
            }
        }
    }

    private suspend fun renderLiveWallpaperPreview(
        context: Context,
        wallpaperModel: LiveWallpaperModel,
        forceSingleEngine: Boolean,
        displaySize: Point,
        engineDisplaySize: Point,
        whichPreview: WhichPreview,
        windowToken: IBinder,
        destinationFlag: Int,
        liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils,
        onEngineReady: (engine: IWallpaperEngine) -> Unit,
        onWallpaperColorsChanged:
            (colors: WallpaperColors?, displayId: Int, persistedColors: WallpaperColors?) -> Unit,
    ): WallpaperSurfaceControl.Live? {
        val connection: LiveWallpaperConnection =
            liveWallpaperConnectionUtils.connect(
                context = context,
                wallpaperModel = wallpaperModel,
                forceSingleEngine = forceSingleEngine,
                destinationFlag = destinationFlag,
                engineDisplaySize = engineDisplaySize,
                windowToken = windowToken,
                displayId = 0, // TODO(b/423956081): give a proper ID here
                whichPreview = whichPreview,
                onEngineReady = onEngineReady,
                onWallpaperColorsChanged = onWallpaperColorsChanged,
            )
        return connection.wallpaperEngine.get()?.mirrorSurfaceControl()?.let {
            WallpaperSurfaceControl.Live(
                liveWallpaperSurfaceControl = it,
                forceSingleEngine = forceSingleEngine,
                displaySize = displaySize,
                engineDisplaySize = engineDisplaySize,
            )
        }
    }

    private fun renderStaticWallpaperPreview(
        applicationContext: Context,
        lifecycleOwner: LifecycleOwner,
        viewModel: WallpaperPreviewViewModel,
        displaySize: Point,
        display: Display,
        hostToken: IBinder,
        onDispatchTouchEventReady: (onDispatchTouchEvent: (event: MotionEvent) -> Unit) -> Unit,
    ): WallpaperSurfaceControl.Static {
        val scale: Float = WallpaperCropUtils.getSystemWallpaperMaximumScale(applicationContext)
        val scaleImageViewSize =
            Point((displaySize.x * scale).toInt(), (displaySize.y * scale).toInt())
        val staticWallpaperPreview =
            LayoutInflater.from(applicationContext).inflate(R.layout.static_wallpaper_preview, null)
        val scaleImageView: SubsamplingScaleImageView =
            staticWallpaperPreview.requireViewById(R.id.static_image_view)
        scaleImageView.apply {
            layoutParams =
                scaleImageView.layoutParams.apply {
                    width = scaleImageViewSize.x
                    height = scaleImageViewSize.y
                }
            setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)

            if (viewModel.wallpaper.value?.isCroppable() == true) {
                initCrop(
                    applicationContext = applicationContext,
                    viewModel = viewModel,
                    displaySize = displaySize,
                    scaleImageViewSize = scaleImageViewSize,
                )
                onDispatchTouchEventReady.invoke(getOnDispatchTouchEvent(this))
            }
        }
        val surfaceControlViewHost =
            SurfaceControlViewHost(applicationContext, display, hostToken).apply {
                setView(staticWallpaperPreview, displaySize.x, displaySize.y)
            }
        StaticWallpaperPreviewBinder2.bind(
            applicationContext = applicationContext,
            scaleImageView = scaleImageView,
            viewModel = viewModel.staticWallpaperPreviewViewModel,
            displaySize = displaySize,
            scaleImageViewSize = scaleImageViewSize,
            lifecycleOwner = lifecycleOwner,
        )
        // TODO(b/423956081): PreviewEffectsLoadingBinder to bind loading effect
        return WallpaperSurfaceControl.Static(surfaceControlViewHost)
    }

    private fun SubsamplingScaleImageView.initCrop(
        applicationContext: Context,
        viewModel: WallpaperPreviewViewModel,
        displaySize: Point,
        scaleImageViewSize: Point,
    ) {
        this.doOnLayout {
            val cropImageSize: Point =
                WallpaperCropUtils.calculateCropSurfaceSize(
                    applicationContext.resources,
                    max(scaleImageViewSize.x, scaleImageViewSize.y),
                    min(scaleImageViewSize.x, scaleImageViewSize.y),
                    scaleImageViewSize.x,
                    scaleImageViewSize.y,
                )
            this.setOnNewCropListener { crop, zoom ->
                viewModel.staticWallpaperPreviewViewModel.fullPreviewCropModels[displaySize] =
                    FullPreviewCropModel(
                        cropHint = crop,
                        cropSizeModel =
                            CropSizeModel(
                                wallpaperZoom = zoom,
                                hostViewSize = scaleImageViewSize,
                                cropViewSize = cropImageSize,
                            ),
                    )
            }
        }
    }

    private fun getOnDispatchTouchEvent(view: View): (event: MotionEvent) -> Unit {
        return { event: MotionEvent -> view.dispatchTouchEvent(event) }
    }

    private fun getOnDispatchTouchEventForLiveWallpapers(
        engine: IWallpaperEngine
    ): (event: MotionEvent) -> Unit {
        // TODO (b/473591862): Create unit test for forwarding touch events to live wallpaper engine
        return { event: MotionEvent ->
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
        previewTarget: PreviewTarget,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        wallpaperColorsModel: WallpaperColorsModel,
        displaySize: Point,
        hostToken: IBinder,
    ): WorkspaceRenderResult? {
        var result: WorkspaceRenderResult? = null
        val workspacePreviewConfig: WorkspacePreviewConfigViewModel =
            wallpaperPreviewViewModel.getWorkspacePreviewConfig(
                screen = previewTarget.screen,
                deviceDisplayType = previewTarget.deviceDisplayType,
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
                reparentLiveWallpaper(
                    surfaceControl = wallpaperSurfaceControl.liveWallpaperSurfaceControl,
                    forceSingleEngine = wallpaperSurfaceControl.forceSingleEngine,
                    displaySize = wallpaperSurfaceControl.displaySize,
                    engineDisplaySize = wallpaperSurfaceControl.engineDisplaySize,
                )
            }
            is WallpaperSurfaceControl.Static -> {
                reparentStaticWallpaper(
                    wallpaperSurfaceControl.staticWallpaperSurfaceControlViewHost
                )
            }
        }
    }

    private fun SurfaceView.reparentLiveWallpaper(
        surfaceControl: SurfaceControl,
        forceSingleEngine: Boolean,
        displaySize: Point,
        engineDisplaySize: Point,
    ) {
        val surfaceViewRootSurfaceControl = this.rootSurfaceControl
        if (surfaceViewRootSurfaceControl == null) {
            Log.w(
                TAG,
                "Unable to reparent workspace SurfaceControl to $this, since SurfaceView's " +
                    "rootSurfaceControl is null.",
            )
            return
        }
        val reparentScale: FloatArray = getReparentScale(displaySize, engineDisplaySize)
        val transaction =
            SurfaceControl.Transaction()
                .setMatrix(
                    surfaceControl,
                    if (forceSingleEngine) reparentScale[Matrix.MSCALE_Y]
                    else reparentScale[Matrix.MSCALE_X],
                    reparentScale[Matrix.MSKEW_X],
                    reparentScale[Matrix.MSKEW_Y],
                    reparentScale[Matrix.MSCALE_Y],
                )
                .reparent(surfaceControl, this.surfaceControl)
                .setLayer(surfaceControl, 0)
                .show(surfaceControl)
        surfaceViewRootSurfaceControl.applyTransactionOnDraw(transaction)
    }

    private fun getReparentScale(displaySize: Point, engineDisplaySize: Point): FloatArray {
        val metrics = Matrix()
        val values = FloatArray(9)
        metrics.postScale(
            displaySize.x.toFloat() / engineDisplaySize.x,
            displaySize.y.toFloat() / engineDisplaySize.y,
        )
        metrics.getValues(values)
        return values
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

        data class Live(
            val liveWallpaperSurfaceControl: SurfaceControl,
            val forceSingleEngine: Boolean,
            val displaySize: Point,
            val engineDisplaySize: Point,
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
