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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.view.SurfaceView
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2

/**
 * Binds the workspace [SurfaceView] with the current lock screen or home screen preview. Also binds
 * any alternative workspace previews.
 */
interface WorkspaceBinder {
    fun bind(
        surfaceView: SurfaceView,
        alternativeWorkspaceView: ComposeView,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    )
}
