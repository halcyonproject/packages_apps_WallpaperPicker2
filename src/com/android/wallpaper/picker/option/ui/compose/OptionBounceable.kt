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

package com.android.wallpaper.picker.option.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.compose.animation.Bounceable

/** Option container bounce animation logic, to be used with `Modifier.bounceable` */
open class OptionBounceable : Bounceable {
    override val bounce: Dp
        get() = effectItemBounceWidth.value

    // This width is manipulated with the clickBounceAnimate function and is added/subtracted
    // onto the width of the effect item buttons.
    private val effectItemBounceWidth = Animatable(0.dp, Dp.VectorConverter)

    /**
     * When an option is chosen, this function gets called and animates the width of the effect
     * button to simulate a bounce effect.
     */
    suspend fun clickBounceAnimate() {
        effectItemBounceWidth.animateTo(
            targetValue = 5.dp,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
        )
        effectItemBounceWidth.animateTo(
            targetValue = 0.dp,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
        )
    }
}
