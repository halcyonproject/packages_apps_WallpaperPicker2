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

package com.android.wallpaper.picker.preview.ui.view

import android.app.wallpaper.WallpaperDescription
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.android.wallpaper.R
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.InformationFloatingSheetViewModel

/**
 * The content used in [PreviewBottomSheet] shows text information of a wallpaper. An explore button
 * is appended if the wallpaper has an uri that leads to an external page.
 */
@Composable
fun PreviewBottomSheetInformationContent(
    informationViewModel: InformationFloatingSheetViewModel,
    logger: UserEventLogger,
) {
    val context = LocalContext.current
    val informationTexts: InformationTexts =
        getInformationTexts(
            attributions = informationViewModel.attributions,
            description = informationViewModel.description,
        )
    val onExploreButtonClicked: (() -> Unit)? =
        (informationViewModel.description?.contextUri ?: informationViewModel.actionUrl?.toUri())
            ?.let { uri ->
                {
                    logger.logWallpaperExploreButtonClicked()
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }

    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        informationTexts.title?.let { title ->
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        informationTexts.subtitle1?.let { subtitle ->
            Text(
                text = subtitle,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        informationTexts.subtitle2?.let { subtitle ->
            Text(
                text = subtitle,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        onExploreButtonClicked?.let {
            // Only get the button title when onExploreButtonClicked is nonnull
            val actionButtonTitle: String =
                (informationViewModel.description?.contextDescription
                        ?: informationViewModel.actionButtonTitle)
                    ?.toString() ?: stringResource(R.string.explore)

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { it.invoke() }, modifier = Modifier.wrapContentWidth()) {
                Text(text = actionButtonTitle)
            }
        }
    }
}

/** The data class wraps the texts which will show in the [PreviewBottomSheetInformationContent]. */
private data class InformationTexts(
    val title: String? = null,
    val subtitle1: String? = null,
    val subtitle2: String? = null,
)

/**
 * Get the information texts that show in the bottom sheet. Note that if the description is not null
 * or empty, it will override attributes, which is a legacy use for showing wallpaper information.
 */
private fun getInformationTexts(
    attributions: List<String>?,
    description: WallpaperDescription?,
): InformationTexts {
    val contentTexts: MutableList<String?> = mutableListOf(null, null, null)
    val contentSize: Int = contentTexts.size

    if (attributions != null) {
        for (i in 0 until minOf(attributions.size, contentSize)) {
            contentTexts[i] = attributions[i].takeIf { it.isNotEmpty() }
        }
    }

    if (description != null) {
        // Assign title to the first part of the info content if title is not null or empty
        description.title.takeIf { !it.isNullOrEmpty() }?.toString()?.let { contentTexts[0] = it }
        // Assign the first 2 texts in description to the second and third part of the info
        // content, if those texts are not empty or null.
        description.description.take(contentSize - 1).forEachIndexed { index, char ->
            char.takeIf { !it.isNullOrEmpty() }?.toString()?.let { contentTexts[index + 1] = it }
        }
    }

    return InformationTexts(
        title = contentTexts[0],
        subtitle1 = contentTexts[1],
        subtitle2 = contentTexts[2],
    )
}
