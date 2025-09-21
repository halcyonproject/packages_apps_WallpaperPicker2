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

package com.android.wallpaper.picker.wallpapers.data.repository

import com.android.wallpaper.model.WallpaperRotationInitializer

/**
 * A factory interface for creating instances of [WallpaperRotationInitializer].
 *
 * This factory is responsible for generating the necessary component to manage wallpaper rotation
 * settings and initiation for a specific wallpaper collection.
 */
interface RotationInitializerFactory {
    /**
     * Creates a new instance of [WallpaperRotationInitializer] for a given wallpaper collection.
     *
     * @param collectionId The unique identifier for the wallpaper collection.
     * @param collectionTitle The display title of the wallpaper collection.
     * @return A [WallpaperRotationInitializer] instance if the collection supports rotation, or
     *   `null` if the collection does not have a rotation feature.
     */
    fun create(collectionId: String, collectionTitle: String): WallpaperRotationInitializer?
}
