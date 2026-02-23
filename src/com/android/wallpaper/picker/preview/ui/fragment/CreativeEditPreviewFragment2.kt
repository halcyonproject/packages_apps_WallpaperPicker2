/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.app.Activity.RESULT_OK
import android.app.wallpaper.WallpaperDescription
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.compose.theme.PlatformTheme
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.WallpaperInfoContract
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.common.preview.ui.binder.PreviewBinder
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.ui.fragment.SmallPreviewFragment.Companion.ARG_EDIT_INTENT
import com.android.wallpaper.picker.preview.ui.fragment.WallpaperPreviewFragment.Companion.init
import com.android.wallpaper.picker.preview.ui.util.ContentHandlingUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel.Companion.EXTRA_WALLPAPER_DESCRIPTION
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.PreviewTarget
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.wallpaperconnection.LiveWallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Shows full preview with an edit activity overlay. */
@AndroidEntryPoint(AppbarFragment::class)
class CreativeEditPreviewFragment2 : Hilt_CreativeEditPreviewFragment2() {

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var liveWallpaperConnectionUtils: LiveWallpaperConnectionUtils

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    private var previewBinding: PreviewBinder.PreviewBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val intent =
            arguments?.getParcelable(ARG_EDIT_INTENT, Intent::class.java)
                ?: throw IllegalArgumentException(
                    "To render the first screen in the create new creative wallpaper flow, the intent for rendering the edit activity overlay can not be null."
                )
        // The repository WallpaperModel is updated every time the creative wallpaper is updated,
        // including when the create new flow is launched. Typically the models are the same, but
        // if we're returning to editing after choosing creative options the repository will have
        // the updated wallpaper, so update the content from there.
        val updatedData =
            (wallpaperPreviewViewModel.wallpaper.value as? LiveWallpaperModel)?.liveWallpaperData
        updatedData?.description?.content?.let { intent.putExtra(EXTRA_WALLPAPER_DESCRIPTION, it) }

        val isCreateNew =
            intent.getBooleanExtra(PreviewActionsViewModel.EXTRA_KEY_IS_CREATE_NEW, false)
        val creativeWallpaperEditActivityResult =
            if (isCreateNew) {
                requireActivity().activityResultRegistry.register(
                    CREATIVE_RESULT_REGISTRY,
                    ActivityResultContracts.StartActivityForResult(),
                ) {
                    // Reset full preview view model to disable full to small preview transition
                    wallpaperPreviewViewModel.resetFullPreviewConfigViewModel()
                    wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper = false
                    // Callback when the overlaying edit activity is finished. Result code of
                    // RESULT_OK means the user clicked on the check button; RESULT_CANCELED
                    // otherwise.
                    if (it.resultCode == RESULT_OK) {
                        // When clicking on the check button, navigate to the preview fragment.
                        findNavController()
                            .navigate(
                                R.id.action_creativeEditPreviewFragment_to_wallpaperPreviewFragment
                            )
                        updatePreview(it.resultCode, it.data)
                    } else {
                        activity?.finish()
                    }
                }
            } else {
                requireActivity().activityResultRegistry.register(
                    CREATIVE_RESULT_REGISTRY,
                    object : ActivityResultContract<Intent, ActivityResult>() {
                        override fun createIntent(context: Context, input: Intent): Intent {
                            return input
                        }

                        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
                            wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper = false
                            updatePreview(resultCode, intent)
                            return ActivityResult(resultCode, intent)
                        }
                    },
                ) {
                    // Reset full preview view model to disable full to small preview transition
                    wallpaperPreviewViewModel.resetFullPreviewConfigViewModel()
                    findNavController().popBackStack()
                }
            }

        if (!wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper) {
            wallpaperPreviewViewModel.isCurrentlyEditingCreativeWallpaper = true
            creativeWallpaperEditActivityResult.launch(intent)
        }

        val deviceDisplayType: DeviceDisplayType =
            displayUtils.getCurrentDisplayType(requireActivity())

        val preview = SurfaceView(context).apply { init() }

        fun bindPreview(rootView: View) {
            val applicationContext: Context = context?.applicationContext ?: return
            val hostToken: IBinder =
                rootView.rootSurfaceControl?.inputTransferToken?.token ?: return
            val windowToken: IBinder = rootView.windowToken ?: return

            previewBinding =
                PreviewBinder.bind(
                    preview = preview,
                    viewModel = wallpaperPreviewViewModel,
                    applicationContext = applicationContext,
                    viewLifecycleOwner = viewLifecycleOwner,
                    previewTarget = PreviewTarget(HOME_SCREEN, deviceDisplayType),
                    displaySizes = wallpaperPreviewViewModel.displaySizes.value,
                    display = requireActivity().display,
                    hostToken = hostToken,
                    windowToken = windowToken,
                    liveWallpaperConnectionUtils = liveWallpaperConnectionUtils,
                    onDispatchTouchEventReady = {},
                    wallpaperPreviewOnly = true,
                )
        }

        // Note that we need to make sure the parent container view is attached to window, so that
        // the surface control's token and the container's window token are ready.
        // The host token is used by the external rendering to listen to its lifecycle, so that when
        // the token is dead, the external rendering can release resources accordingly.
        if (container?.isAttachedToWindow == true) {
            bindPreview(container)
        } else {
            container?.addOnAttachStateChangeListener(
                object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        container.removeOnAttachStateChangeListener(this)
                        bindPreview(container)
                    }

                    override fun onViewDetachedFromWindow(p0: View) {
                        // Do nothing intended
                    }
                }
            )
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { PlatformTheme { FullWallpaperPreview(preview = preview) } }
        }
    }

    // Updates the current preview using the WallpaperDescription returned with the Intent if any
    private fun updatePreview(resultCode: Int, intent: Intent?) {
        if (resultCode == RESULT_OK) {
            wallpaperPreviewViewModel.wallpaper.value?.let {
                ContentHandlingUtil.updatePreview(
                    context = appContext,
                    wallpaperModel = it,
                    wallpaperDescription =
                        intent
                            ?.extras
                            ?.getParcelable(
                                WallpaperInfoContract.WALLPAPER_DESCRIPTION_CONTENT_HANDLING,
                                WallpaperDescription::class.java,
                            ),
                ) { wallpaperModel ->
                    wallpaperPreviewViewModel.setPreviewWallpaperModel(wallpaperModel)
                }
            }
        }
    }

    override fun onDestroyView() {
        previewBinding?.releasePreview()
        previewBinding = null
        wallpaperPreviewViewModel.resetPreviews()
        super.onDestroyView()
    }

    @Composable
    fun FullWallpaperPreview(preview: SurfaceView, modifier: Modifier = Modifier) {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { preview })
        }
    }

    companion object {
        private const val CREATIVE_RESULT_REGISTRY = "creative_result_registry"
    }
}
