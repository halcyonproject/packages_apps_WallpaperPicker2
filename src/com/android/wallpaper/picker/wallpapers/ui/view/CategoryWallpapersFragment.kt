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

package com.android.wallpaper.picker.wallpapers.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.compose.theme.PlatformTheme
import com.android.wallpaper.R
import com.android.wallpaper.model.WallpaperRotationInitializer.NETWORK_PREFERENCE_WIFI_ONLY
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2.Companion.CUSTOMIZATION_PICKER_FRAGMENT_TAG
import com.android.wallpaper.picker.customization.ui.CustomizationPickerFragment2
import com.android.wallpaper.picker.wallpapers.ui.view.compose.LoadingSpinner
import com.android.wallpaper.picker.wallpapers.ui.view.compose.WallpapersScreenContent
import com.android.wallpaper.picker.wallpapers.ui.view.viewmodel.CategoryWallpapersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** This fragment displays the collection of wallpapers for the selected category */
@AndroidEntryPoint(AppbarFragment::class)
class CategoryWallpapersFragment : Hilt_CategoryWallpapersFragment() {

    private val viewModel: CategoryWallpapersViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dismissScreenEvent.collect {
                    val fragmentManager: FragmentManager = parentFragmentManager
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                    // Ensure the root fragment is CUSTOMIZATION_PICKER_FRAGMENT_TAG
                    if (
                        fragmentManager.findFragmentByTag(CUSTOMIZATION_PICKER_FRAGMENT_TAG) == null
                    ) {
                        fragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.fragment_container,
                                CustomizationPickerFragment2(),
                                CUSTOMIZATION_PICKER_FRAGMENT_TAG,
                            )
                            .commit()
                    }
                }
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val categoryWallpapersContentViewModel by
                    viewModel.categoryWallpapersContentViewModel.collectAsStateWithLifecycle(null)

                val areWallpapersLoading by
                    viewModel.categoryWallpapersIsLoading.collectAsStateWithLifecycle(true)

                val isRotationDialogShowing by
                    viewModel.showRotationDialog.collectAsStateWithLifecycle(false)

                val startRotationInProgress by
                    viewModel.isRotationLoading.collectAsStateWithLifecycle(false)

                val networkPreference by
                    viewModel.networkPreference.collectAsStateWithLifecycle(
                        NETWORK_PREFERENCE_WIFI_ONLY
                    )
                PlatformTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        categoryWallpapersContentViewModel?.let { viewModel ->
                            WallpapersScreenContent(
                                viewModel = viewModel,
                                isRotationDialogShowing = isRotationDialogShowing,
                                networkPreference = networkPreference,
                                isRotationLoading = startRotationInProgress,
                            )
                        }

                        LoadingSpinner(
                            isLoading = areWallpapersLoading,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CategoryWallpapersFragment"

        fun newInstance(): CategoryWallpapersFragment {
            val fragment = CategoryWallpapersFragment()
            return fragment
        }
    }
}
