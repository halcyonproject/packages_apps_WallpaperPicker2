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

package com.android.wallpaper.picker.preview.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CHECKBOX_SIZE = 20.dp
private val CHECKBOX_BORDER_SIZE = 2.dp
private val CHECKBOX_PADDING = 14.dp
private val CHECKBOX_LABEL_SIZE = 16.sp

/** A checkbox with a label. */
@Composable
fun LabelCheckbox(
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier.toggleable(
                value = isChecked,
                onValueChange = { onCheckedChange.invoke() },
                role = Role.Checkbox,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.padding(CHECKBOX_PADDING)
                    .size(CHECKBOX_SIZE)
                    .then(
                        if (isChecked) {
                            Modifier.background(colorScheme.primary, CircleShape)
                        } else {
                            Modifier.border(
                                width = CHECKBOX_BORDER_SIZE,
                                color = colorScheme.primary,
                                shape = CircleShape,
                            )
                        }
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Checkbox(
                modifier = Modifier.size(CHECKBOX_SIZE),
                checked = isChecked,
                onCheckedChange = null,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = Color.Transparent,
                        uncheckedColor = Color.Transparent,
                    ),
            )
        }
        Text(text = text, fontSize = CHECKBOX_LABEL_SIZE, color = colorScheme.onSurface)
    }
}
