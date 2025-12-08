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
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.icon.shared.model.IconStyle
import com.android.customization.picker.icon.shared.model.IconStyleModel
import com.android.customization.picker.icon.ui.viewmodel.ShapeIconViewModel
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlinx.coroutines.DisposableHandle

@ActivityScoped
class DefaultIconStyleViewUtil @Inject constructor() : IconStyleViewUtil {
    override fun getOnClick(iconStyle: IconStyle): (() -> Unit)? {
        TODO("Not yet implemented")
    }

    override fun bindIconOptionView(
        view: View,
        iconStyleModel: IconStyleModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle? {
        TODO("Not yet implemented")
    }

    override fun bindShapeIconPreview(
        view: View,
        iconStyleModel: IconStyleModel?,
        shapeIcon: ShapeIconViewModel?,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle? {
        TODO("Not yet implemented")
    }

    override fun bindIconColors(
        iconStyleModel: IconStyleModel,
        icon: Icon,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle? {
        TODO("Not yet implemented")
    }

    override fun getIcon(iconStyleModel: IconStyleModel?, shapePath: String?): Icon? {
        TODO("Not yet implemented")
    }
}
