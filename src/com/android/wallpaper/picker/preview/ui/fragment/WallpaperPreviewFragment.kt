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
package com.android.wallpaper.picker.preview.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.DefaultElementContentPicker
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.SceneTransitions
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.rememberMutableSceneTransitionLayoutState
import com.android.compose.animation.scene.transitions
import com.android.compose.theme.PlatformTheme
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.FOLDED
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.SINGLE
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.UNFOLDED
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.common.preview.ui.binder.PreviewBinder
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2
import com.android.wallpaper.picker.preview.ui.util.AnimationUtil
import com.android.wallpaper.picker.preview.ui.view.ApplyWallpaperScene
import com.android.wallpaper.picker.preview.ui.view.FullWallpaperPreviewScene
import com.android.wallpaper.picker.preview.ui.view.SmallWallpaperPreviewScene
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.DisplaySizes
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.PreviewTarget
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.ExtendedWallpaperEffectsUtils
import com.android.wallpaper.util.LaunchSourceUtils.LAUNCH_SOURCE_LAUNCHER
import com.android.wallpaper.util.LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE
import com.android.wallpaper.util.wallpaperconnection.LiveWallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This fragment hosts the wallpaper preview screen. The screen has two major functions:
 * 1. preview wallpapers: it displays the preview of the selected wallpaper on all available
 *    workspaces and devices displays.
 * 2. apply wallpapers: users can apply the wallpaper to the devices.
 *
 * [WallpaperPreviewFragment] can only be used by refactor_wallpaper_previewScreen_flag.
 */
@AndroidEntryPoint(AppbarFragment::class)
class WallpaperPreviewFragment : Hilt_WallpaperPreviewFragment() {

    enum class SceneIdentity {
        SmallPreview,
        FullLockPreview,
        FullLockUnfoldedPreview,
        FullHomePreview,
        FullHomeUnfoldedPreview,
        ApplyWallpaper,
    }

    object Scenes {
        val SmallPreview = SceneKey(debugName = "SmallPreviewScene", SceneIdentity.SmallPreview)
        val FullLockPreview =
            SceneKey(debugName = "FullLockPreviewScene", SceneIdentity.FullLockPreview)
        val FullLockUnfoldedPreview =
            SceneKey(
                debugName = "FullLockUnfoldedPreviewScene",
                SceneIdentity.FullLockUnfoldedPreview,
            )
        val FullHomePreview =
            SceneKey(debugName = "FullHomePreviewScene", SceneIdentity.FullHomePreview)
        val FullHomeUnfoldedPreview =
            SceneKey(
                debugName = "FullHomeUnfoldedPreviewScene",
                SceneIdentity.FullHomeUnfoldedPreview,
            )
        val ApplyWallpaper =
            SceneKey(debugName = "ApplyWallpaperScene", SceneIdentity.ApplyWallpaper)
    }

    object Elements {
        val SmallPreviewTopToolbar = ElementKey(debugName = "SmallPreviewTopToolbar")
        val SmallPreviewBottomActionBar = ElementKey(debugName = "SmallPreviewBottomActionBar")
        val FullPreviewTopToolbar = ElementKey(debugName = "FullPreviewTopToolbar")
        val FullPreviewBackground = ElementKey(debugName = "FullPreviewBackground")
        val ApplyWallpaperTitle = ElementKey(debugName = "ApplyWallpaperTitle")
        val ApplyWallpaperLockScreenCheckbox =
            ElementKey(debugName = "ApplyWallpaperLockScreenCheckbox")
        val ApplyWallpaperHomeScreenCheckbox =
            ElementKey(debugName = "ApplyWallpaperHomeScreenCheckbox")
        val ApplyWallpaperBottomButtons = ElementKey(debugName = "ApplyWallpaperBottomButtons")
    }

    object SharedElements {
        val LockScreen =
            MovableElementKey(
                debugName = "LockScreen",
                contentPicker =
                    DefaultElementContentPicker(
                        contents =
                            setOf(
                                Scenes.SmallPreview,
                                Scenes.FullLockPreview,
                                Scenes.ApplyWallpaper,
                            )
                    ),
            )
        val LockScreenUnfolded =
            MovableElementKey(
                debugName = "LockScreenUnfolded",
                contentPicker =
                    DefaultElementContentPicker(
                        contents =
                            setOf(
                                Scenes.SmallPreview,
                                Scenes.FullLockUnfoldedPreview,
                                Scenes.ApplyWallpaper,
                            )
                    ),
            )
        val HomeScreen =
            MovableElementKey(
                debugName = "HomeScreen",
                contentPicker =
                    DefaultElementContentPicker(
                        contents =
                            setOf(
                                Scenes.SmallPreview,
                                Scenes.FullHomePreview,
                                Scenes.ApplyWallpaper,
                            )
                    ),
            )
        val HomeScreenUnfolded =
            MovableElementKey(
                debugName = "HomeScreenUnfolded",
                contentPicker =
                    DefaultElementContentPicker(
                        contents =
                            setOf(
                                Scenes.SmallPreview,
                                Scenes.FullHomeUnfoldedPreview,
                                Scenes.ApplyWallpaper,
                            )
                    ),
            )
    }

    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils
    @Inject lateinit var logger: UserEventLogger

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    private var previewBindings: List<PreviewBinder.PreviewBinding>? = null

    private val shareActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val fullLockPreviewOnDispatchTouchEventFlow:
        MutableStateFlow<((event: MotionEvent) -> Unit)?> =
        MutableStateFlow(null)
    private val fullLockUnfoldedPreviewOnDispatchTouchEventFlow:
        MutableStateFlow<((event: MotionEvent) -> Unit)?> =
        MutableStateFlow(null)
    private val fullHomePreviewOnDispatchTouchEventFlow:
        MutableStateFlow<((event: MotionEvent) -> Unit)?> =
        MutableStateFlow(null)
    private val fullHomeUnfoldedPreviewOnDispatchTouchEventFlow:
        MutableStateFlow<((event: MotionEvent) -> Unit)?> =
        MutableStateFlow(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BaseFlags.get(requireContext()).isRefactorWallpaperPreviewScreenEnabled()) {
            throw IllegalStateException(
                "$this can only be used when " +
                    "refactor_wallpaper_preview_screen_flag is turned on."
            )
        }
        exitTransition = AnimationUtil.getFastFadeOutTransition()
        reenterTransition = AnimationUtil.getFastFadeInTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val isFoldable = displayUtils.hasMultiInternalDisplays()

        val displaySizes: DisplaySizes = wallpaperPreviewViewModel.displaySizes.value
        val lockScreenPreview = SurfaceView(context)
        val homeScreenPreview = SurfaceView(context)
        val lockScreenUnfoldedPreview: SurfaceView? = if (isFoldable) SurfaceView(context) else null
        val homeScreenUnfoldedPreview: SurfaceView? = if (isFoldable) SurfaceView(context) else null
        val onDispatchTouchEventReady:
            (
                previewTarget: PreviewTarget, onDispatchTouchEvent: (event: MotionEvent) -> Unit,
            ) -> Unit =
            { targetPreview, onDispatchTouchEvent ->
                when (targetPreview.screen) {
                    LOCK_SCREEN ->
                        when (targetPreview.deviceDisplayType) {
                            SINGLE,
                            FOLDED ->
                                fullLockPreviewOnDispatchTouchEventFlow.value = onDispatchTouchEvent
                            UNFOLDED ->
                                fullLockUnfoldedPreviewOnDispatchTouchEventFlow.value =
                                    onDispatchTouchEvent
                        }
                    HOME_SCREEN ->
                        when (targetPreview.deviceDisplayType) {
                            SINGLE,
                            FOLDED ->
                                fullHomePreviewOnDispatchTouchEventFlow.value = onDispatchTouchEvent
                            UNFOLDED ->
                                fullHomeUnfoldedPreviewOnDispatchTouchEventFlow.value =
                                    onDispatchTouchEvent
                        }
                }
            }

        fun bindPreviews(rootView: View) {
            val applicationContext: Context = context?.applicationContext ?: return
            val hostToken: IBinder =
                rootView.rootSurfaceControl?.inputTransferToken?.token ?: return
            val windowToken: IBinder = rootView.windowToken ?: return
            previewBindings?.forEach { it.releasePreview() }
            previewBindings =
                if (isFoldable)
                    PreviewBinder.bindFoldablePreviews(
                        applicationContext = applicationContext,
                        viewModel = wallpaperPreviewViewModel,
                        viewLifecycleOwner = viewLifecycleOwner,
                        liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                        display = requireActivity().display,
                        hostToken = hostToken,
                        windowToken = windowToken,
                        lockScreenPreview = lockScreenPreview,
                        lockScreenUnfoldedPreview =
                            checkNotNull(lockScreenUnfoldedPreview) {
                                "lockScreenUnfoldedPreview can not be null for foldables."
                            },
                        homeScreenPreview = homeScreenPreview,
                        homeScreenUnfoldedPreview =
                            checkNotNull(homeScreenUnfoldedPreview) {
                                "lockScreenUnfoldedPreview can not be null for foldables."
                            },
                        displaySizes = displaySizes,
                        onDispatchTouchEventReady = onDispatchTouchEventReady,
                    )
                else
                    PreviewBinder.bindSinglePreviews(
                        applicationContext = applicationContext,
                        viewModel = wallpaperPreviewViewModel,
                        viewLifecycleOwner = viewLifecycleOwner,
                        liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                        display = requireActivity().display,
                        hostToken = hostToken,
                        windowToken = windowToken,
                        lockScreenPreview = lockScreenPreview,
                        homeScreenPreview = homeScreenPreview,
                        displaySizes = displaySizes,
                        onDispatchTouchEventReady = onDispatchTouchEventReady,
                    )
        }

        // Note that we need to make sure the parent container view is attached to window, so that
        // the surface control's token and the container's window token are ready.
        // The host token is used by the external rendering to listen to its lifecycle, so that when
        // the token is dead, the external rendering can release resources accordingly.
        if (container?.isAttachedToWindow == true) {
            bindPreviews(container)
        } else {
            container?.addOnAttachStateChangeListener(
                object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        bindPreviews(container)
                    }

                    override fun onViewDetachedFromWindow(p0: View) {
                        // Do nothing intended
                    }
                }
            )
        }

        val extendedWallpaperEffectActivityLauncher: ActivityResultLauncher<Intent> =
            ExtendedWallpaperEffectsUtils.registerExtendedWallpaperEffectsActivityLauncher(
                activity = requireActivity(),
                lifecycleOwner = viewLifecycleOwner,
                wallpaperPreviewViewModel = wallpaperPreviewViewModel,
                context = requireContext(),
                exitActivityOnCancel = wallpaperPreviewViewModel.launchedForWallpaperEffects,
            )

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PlatformTheme {
                    WallpaperPreviewRootContent(
                        isFoldable = isFoldable,
                        lockScreenPreview = lockScreenPreview,
                        lockScreenUnfoldedPreview = lockScreenUnfoldedPreview,
                        homeScreenPreview = homeScreenPreview,
                        homeScreenUnfoldedPreview = homeScreenUnfoldedPreview,
                        displaySizes = displaySizes,
                        extendedWallpaperEffectActivityLauncher =
                            extendedWallpaperEffectActivityLauncher,
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        previewBindings?.forEach { it.releasePreview() }
        previewBindings = null
        Screen.entries.forEach { screen ->
            DeviceDisplayType.entries.forEach { deviceDisplayType ->
                wallpaperPreviewViewModel.setPreviewReady2(
                    PreviewTarget(screen, deviceDisplayType),
                    false,
                )
            }
        }
        super.onDestroyView()
    }

    @Composable
    fun WallpaperPreviewRootContent(
        isFoldable: Boolean,
        lockScreenPreview: SurfaceView,
        lockScreenUnfoldedPreview: SurfaceView?,
        homeScreenPreview: SurfaceView,
        homeScreenUnfoldedPreview: SurfaceView?,
        displaySizes: DisplaySizes,
        extendedWallpaperEffectActivityLauncher: ActivityResultLauncher<Intent>,
        modifier: Modifier = Modifier,
    ) {
        var initialSceneIdentity by rememberSaveable { mutableStateOf(SceneIdentity.SmallPreview) }
        val initialScene =
            getInitialScene(initialSceneIdentity, activity?.isInMultiWindowMode == true)
        val sceneState =
            rememberMutableSceneTransitionLayoutState(
                initialScene = initialScene,
                transitions = remember { sceneTransitions(isFoldable) },
            )

        LaunchedEffect(sceneState.currentScene) {
            initialSceneIdentity = sceneState.currentScene.identity as SceneIdentity
            wallpaperPreviewViewModel.setShouldForceDesktopFullscreen(
                !isSceneCompatibleWithWindowedMode(sceneState.currentScene)
            )
        }

        // Pager state needs to be outside the scope of the SceneTransitionLayout so that after
        // transitioning back to the small preview scene, the selected page index can be retained.
        val pagerState: PagerState =
            rememberPagerState(
                initialPage = wallpaperPreviewViewModel.getSmallPreviewTabIndex(),
                pageCount = { wallpaperPreviewViewModel.smallPreviewTabs.size },
            )
        // This is only needed for the apply wallpaper scene on foldables. We need a separate
        // PagerState for the ApplyWallpaperScene because when alwaysCompose is true, the scene
        // is composed even when not active. Having a separate PagerState allows the
        // ApplyWallpaperScene to manage its pager independently while still syncing with the
        // main pagerState used in SmallPreviewScene.
        val applyWallpaperScenePagerState: PagerState? =
            if (isFoldable) {
                rememberPagerState(
                        initialPage = pagerState.currentPage,
                        pageCount = { pagerState.pageCount },
                    )
                    .also { LaunchedSyncPagerState(pagerState, it) }
            } else {
                null
            }

        val fullLockPreviewOnDispatchTouchEvent: ((event: MotionEvent) -> Unit)? by
            fullLockPreviewOnDispatchTouchEventFlow.collectAsStateWithLifecycle(null)
        val fullLockUnfoldedPreviewOnDispatchTouchEvent: ((event: MotionEvent) -> Unit)? by
            fullLockUnfoldedPreviewOnDispatchTouchEventFlow.collectAsStateWithLifecycle(null)
        val fullHomePreviewOnDispatchTouchEvent: ((event: MotionEvent) -> Unit)? by
            fullHomePreviewOnDispatchTouchEventFlow.collectAsStateWithLifecycle(null)
        val fullHomeUnfoldedPreviewOnDispatchTouchEvent: ((event: MotionEvent) -> Unit)? by
            fullHomeUnfoldedPreviewOnDispatchTouchEventFlow.collectAsStateWithLifecycle(null)

        SceneTransitionLayout(state = sceneState, modifier = modifier) {
            // The order of the scene here matters. During transitions the first defined scene will
            // be drawn below the second, scene, which will be drawn below the third one, etc.
            scene(Scenes.SmallPreview) {
                SmallWallpaperPreviewScene(
                    isFoldable = isFoldable,
                    viewModel = wallpaperPreviewViewModel,
                    sceneState = sceneState,
                    pagerState = pagerState,
                    lockScreenPreview = lockScreenPreview,
                    lockScreenUnfoldedPreview = lockScreenUnfoldedPreview,
                    homeScreenPreview = homeScreenPreview,
                    homeScreenUnfoldedPreview = homeScreenUnfoldedPreview,
                    displaySizes = displaySizes,
                    logger = logger,
                    onFinishActivity = { activity?.finish() },
                    onNavigateToEditScreen = { intent ->
                        findNavController()
                            .navigate(
                                resId =
                                    R.id
                                        .action_wallpaperPreviewFragment_to_creativeEditPreviewFragment,
                                args = Bundle().apply { putParcelable(ARG_EDIT_INTENT, intent) },
                            )
                    },
                    onStartShareActivity = { intent -> shareActivityResultLauncher.launch(intent) },
                    extendedWallpaperEffectActivityLauncher =
                        extendedWallpaperEffectActivityLauncher,
                )
            }
            // Apply wallpaper scene
            scene(
                key = Scenes.ApplyWallpaper,
                userActions = mapOf(Back to Scenes.SmallPreview),
                // For foldables, we need to compose the scene first since the scene is
                // comparatively heavier to compose. It can introduce jank if it composes on the fly
                // when transition starts.
                alwaysCompose = isFoldable,
            ) {
                ApplyWallpaperScene(
                    isFoldable = isFoldable,
                    viewModel = wallpaperPreviewViewModel,
                    sceneState = sceneState,
                    foldablePreviewPagerState = applyWallpaperScenePagerState,
                    lockScreenPreview = lockScreenPreview,
                    lockScreenUnfoldedPreview = lockScreenUnfoldedPreview,
                    homeScreenPreview = homeScreenPreview,
                    homeScreenUnfoldedPreview = homeScreenUnfoldedPreview,
                    displaySizes = displaySizes,
                    onWallpaperApplied = {
                        Toast.makeText(
                                context,
                                R.string.wallpaper_set_successfully_message,
                                Toast.LENGTH_SHORT,
                            )
                            .show()

                        activity?.let { activityReference ->
                            val intent =
                                Intent(activityReference, CustomizationPickerActivity2::class.java)
                            // Clear the whole Activity stack and restart the
                            // CustomizationPickerActivity2 to make sure to go back to the main
                            // screen.
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            if (wallpaperPreviewViewModel.isViewAsHome) {
                                intent.putExtra(WALLPAPER_LAUNCH_SOURCE, LAUNCH_SOURCE_LAUNCHER)
                            }
                            activityReference.startActivity(intent)
                            activityReference.finish()
                        }
                    },
                )
            }
            // Full preview scene
            if (isFoldable) {
                scene(Scenes.FullLockPreview, userActions = mapOf(Back to Scenes.SmallPreview)) {
                    FullWallpaperPreviewScene(
                        viewModel = wallpaperPreviewViewModel,
                        sceneState = sceneState,
                        previewTarget = PreviewTarget(LOCK_SCREEN, FOLDED),
                        displaySizes = displaySizes,
                        preview = lockScreenPreview,
                        onDispatchTouchEvent = fullLockPreviewOnDispatchTouchEvent,
                    )
                }
                scene(Scenes.FullHomePreview, userActions = mapOf(Back to Scenes.SmallPreview)) {
                    FullWallpaperPreviewScene(
                        viewModel = wallpaperPreviewViewModel,
                        sceneState = sceneState,
                        previewTarget = PreviewTarget(HOME_SCREEN, FOLDED),
                        displaySizes = displaySizes,
                        preview = homeScreenPreview,
                        onDispatchTouchEvent = fullHomePreviewOnDispatchTouchEvent,
                    )
                }
                scene(
                    Scenes.FullLockUnfoldedPreview,
                    userActions = mapOf(Back to Scenes.SmallPreview),
                ) {
                    FullWallpaperPreviewScene(
                        viewModel = wallpaperPreviewViewModel,
                        sceneState = sceneState,
                        previewTarget = PreviewTarget(LOCK_SCREEN, UNFOLDED),
                        displaySizes = displaySizes,
                        preview = checkNotNull(lockScreenUnfoldedPreview),
                        onDispatchTouchEvent = fullLockUnfoldedPreviewOnDispatchTouchEvent,
                    )
                }
                scene(
                    Scenes.FullHomeUnfoldedPreview,
                    userActions = mapOf(Back to Scenes.SmallPreview),
                ) {
                    FullWallpaperPreviewScene(
                        viewModel = wallpaperPreviewViewModel,
                        sceneState = sceneState,
                        previewTarget = PreviewTarget(HOME_SCREEN, UNFOLDED),
                        displaySizes = displaySizes,
                        preview = checkNotNull(homeScreenUnfoldedPreview),
                        onDispatchTouchEvent = fullHomeUnfoldedPreviewOnDispatchTouchEvent,
                    )
                }
            } else {
                scene(Scenes.FullLockPreview, userActions = mapOf(Back to Scenes.SmallPreview)) {
                    FullWallpaperPreviewScene(
                        viewModel = wallpaperPreviewViewModel,
                        sceneState = sceneState,
                        previewTarget = PreviewTarget(LOCK_SCREEN, SINGLE),
                        displaySizes = displaySizes,
                        preview = lockScreenPreview,
                        onDispatchTouchEvent = fullLockPreviewOnDispatchTouchEvent,
                    )
                }
                scene(Scenes.FullHomePreview, userActions = mapOf(Back to Scenes.SmallPreview)) {
                    FullWallpaperPreviewScene(
                        viewModel = wallpaperPreviewViewModel,
                        sceneState = sceneState,
                        previewTarget = PreviewTarget(HOME_SCREEN, SINGLE),
                        displaySizes = displaySizes,
                        preview = homeScreenPreview,
                        onDispatchTouchEvent = fullHomePreviewOnDispatchTouchEvent,
                    )
                }
            }
        }
    }

    private fun sceneTransitions(isFoldable: Boolean): SceneTransitions {
        return transitions {
            from(Scenes.SmallPreview, to = Scenes.ApplyWallpaper) {
                spec = tween(durationMillis = 350)
                timestampRange(endMillis = 150) {
                    fade(Elements.SmallPreviewTopToolbar)
                    fade(Elements.SmallPreviewBottomActionBar)
                }
                timestampRange(startMillis = 200) {
                    fade(Elements.ApplyWallpaperTitle)
                    fade(Elements.ApplyWallpaperLockScreenCheckbox)
                    fade(Elements.ApplyWallpaperHomeScreenCheckbox)
                    fade(Elements.ApplyWallpaperBottomButtons)
                }
            }
            from(Scenes.SmallPreview, to = Scenes.FullLockPreview) {
                smallPreviewToFullPreviewTransitionSpec()
            }
            from(Scenes.SmallPreview, to = Scenes.FullHomePreview) {
                smallPreviewToFullPreviewTransitionSpec()
            }
            if (isFoldable) {
                from(Scenes.SmallPreview, to = Scenes.FullLockUnfoldedPreview) {
                    smallPreviewToFullPreviewTransitionSpec()
                }
                from(Scenes.SmallPreview, to = Scenes.FullHomeUnfoldedPreview) {
                    smallPreviewToFullPreviewTransitionSpec()
                }
            }
        }
    }

    private fun TransitionBuilder.smallPreviewToFullPreviewTransitionSpec() {
        spec = tween(durationMillis = 350)
        timestampRange(endMillis = 150) {
            fade(Elements.SmallPreviewTopToolbar)
            fade(Elements.SmallPreviewBottomActionBar)
        }
        timestampRange(startMillis = 200) {
            fade(Elements.FullPreviewTopToolbar)
            fade(Elements.FullPreviewBackground)
        }
    }

    @Composable
    private fun LaunchedSyncPagerState(pagerStateA: PagerState, pagerStateB: PagerState) {
        LaunchedEffect(pagerStateA, pagerStateB) {
            snapshotFlow {
                    // Pair the current pages of both states
                    pagerStateA.currentPage to pagerStateB.currentPage
                }
                .collect { (currentPageA, currentPageB) ->
                    if (currentPageA != currentPageB) {
                        // Sync the 'inactive' pager to the primary.
                        if (pagerStateA.isScrollInProgress) {
                            pagerStateB.scrollToPage(currentPageA)
                        } else if (pagerStateB.isScrollInProgress) {
                            pagerStateA.scrollToPage(currentPageB)
                        } else {
                            // This is to prevent infinite loop if both of the pager states
                            // are not scroll in progress, which should not happen.
                            pagerStateB.scrollToPage(currentPageA)
                        }
                    }
                }
        }
    }

    private fun getInitialScene(
        sceneIdentity: SceneIdentity,
        isInMultiWindowMode: Boolean,
    ): SceneKey {
        val sceneKey = getSceneKeyFromSceneIdentity(sceneIdentity)
        if (!isInMultiWindowMode) {
            return sceneKey
        }
        return if (isSceneCompatibleWithWindowedMode(sceneKey)) sceneKey else Scenes.SmallPreview
    }

    private fun getSceneKeyFromSceneIdentity(sceneIdentity: SceneIdentity): SceneKey {
        return when (sceneIdentity) {
            SceneIdentity.SmallPreview -> Scenes.SmallPreview
            SceneIdentity.ApplyWallpaper -> Scenes.ApplyWallpaper
            SceneIdentity.FullLockPreview -> Scenes.FullLockPreview
            SceneIdentity.FullLockUnfoldedPreview -> Scenes.FullLockUnfoldedPreview
            SceneIdentity.FullHomePreview -> Scenes.FullHomePreview
            SceneIdentity.FullHomeUnfoldedPreview -> Scenes.FullHomeUnfoldedPreview
        }
    }

    companion object {
        const val ARG_EDIT_INTENT = "arg_edit_intent"

        fun isSceneCompatibleWithWindowedMode(sceneKey: SceneKey): Boolean {
            return when (sceneKey) {
                Scenes.FullLockPreview,
                Scenes.FullLockUnfoldedPreview,
                Scenes.FullHomePreview,
                Scenes.FullHomeUnfoldedPreview -> false
                else -> true
            }
        }
    }
}
