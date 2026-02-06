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

package com.android.customization.picker.clock.ai

import android.content.Context
import com.android.customization.picker.clock.model.ClockStyleModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2

/**
 * Utility interface for managing and providing view models related to clock style customizations.
 *
 * This utility serves as the bridge between the clock customization logic and the UI layer,
 * specifically handling the creation of entry points for generative or advanced clock styles.
 */
interface ClockStyleViewUtil {

    /**
     * Returns the [OptionItemViewModel2] used as the entry point for clock style customization.
     *
     * @param context The application or activity context used for resource resolution.
     * @return An [OptionItemViewModel2] containing the [ClockPickerViewModel.ClockStyleModel]
     *   payload if the entry point should be visible; null otherwise (e.g., if the feature is
     *   disabled or flags are off).
     */
    fun getEntryPointOptionViewModel(context: Context): OptionItemViewModel2<ClockStyleModel>?
}
