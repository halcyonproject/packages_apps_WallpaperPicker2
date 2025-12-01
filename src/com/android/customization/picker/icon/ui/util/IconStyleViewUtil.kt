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

package com.android.customization.picker.icon.ui.util

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.icon.shared.model.IconStyle
import com.android.customization.picker.icon.shared.model.IconStyleModel
import com.android.customization.picker.icon.ui.viewmodel.ShapeIconViewModel
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import kotlinx.coroutines.DisposableHandle

interface IconStyleViewUtil : DefaultLifecycleObserver {
    fun getOnClick(iconStyle: IconStyle): (() -> Unit)?

    /** Binds an icon style option view for a specific icon style */
    fun bindIconOptionView(
        view: View,
        iconStyleModel: IconStyleModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle?

    /** Binds a icon preview thumbnail with the selected icon style and shape */
    fun bindShapeIconPreview(
        view: View,
        iconStyleModel: IconStyleModel?,
        shapeIcon: ShapeIconViewModel? = null,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle?

    /** Binds the color of an icon based on the current UI colors, if needed */
    fun bindIconColors(
        iconStyleModel: IconStyleModel,
        icon: Icon,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle?

    /** Gets an icon without any color binding */
    fun getIcon(iconStyleModel: IconStyleModel): Icon?
}
