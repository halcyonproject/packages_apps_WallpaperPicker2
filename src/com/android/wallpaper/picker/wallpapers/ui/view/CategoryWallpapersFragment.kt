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
import androidx.compose.ui.platform.ComposeView
import com.android.wallpaper.picker.AppbarFragment
import dagger.hilt.android.AndroidEntryPoint

/** This fragment displays the collection of wallpapers for the selected category */
@AndroidEntryPoint(AppbarFragment::class)
class CategoryWallpapersFragment : Hilt_CategoryWallpapersFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            // Call the bind method here
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
