/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.model

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import com.android.wallpaper.asset.Asset

/** Wallpaper category model object. */
abstract class Category
@JvmOverloads
constructor(
    /** The title of the category. */
    val title: String,
    /** The ID of the collection this category represents. */
    val collectionId: String,
    /**
     * Returns the relative priority of the category. The lower the number, the higher the priority.
     */
    val priority: Int,
    /** Returns whether this category has downloadable wallpapers */
    val isCategoryDownloadable: Boolean = false,
    /** Returns this [Category]'s download component */
    val categoryDownloadComponent: String? = null,
) {

    /** Returns whether user created wallpapers are supported or not. */
    open fun supportsUserCreatedWallpapers(context: Context): Boolean {
        return false
    }

    /**
     * Shows the UI for picking wallpapers within this category.
     *
     * @param srcActivity
     * @param requestCode Request code to pass in when starting the picker activity.
     */
    abstract fun show(srcActivity: Activity?, requestCode: Int)

    open val isEnumerable: Boolean
        /**
         * Returns true if this Category contains an enumerable set of wallpapers which can be
         * presented by a UI enclosed in an activity. Returns false if, by contrast, this Category
         * must be presented via #show() because its contents are not enumerable.
         */
        get() = false

    open val isSingleWallpaperCategory: Boolean
        /**
         * Returns true if this category contains a single Wallpaper, which could then be retrieved
         * via [.getSingleWallpaper]
         */
        get() = false

    open val singleWallpaper: WallpaperInfo?
        /**
         * If [.isSingleWallpaperCategory] returned true, this method will return the single
         * wallpaper contained in this category.
         *
         * @return a [WallpaperInfo] for the one wallpaper in this category, if this category is a
         *   single wallpaper category, or `null` otherwise.
         */
        get() = null

    /**
     * Returns the overlay icon. Takes an application's Context if a Category needs to query for
     * what resources may be available on the device (for example, querying permissions).
     */
    open fun getOverlayIcon(unused: Context?): Drawable? {
        return null
    }

    open val overlayIconSizeDp: Int
        /**
         * Returns the desired size of the overlay icon in density-independent pixels. Default value
         * is 40.
         */
        get() = 40

    open val wallpaperRotationInitializer: WallpaperRotationInitializer?
        /**
         * Returns the [WallpaperRotationInitializer] for this category or null if rotation is not
         * enabled for this category.
         */
        get() = null

    /**
     * Returns the thumbnail Asset. Takes an application's Context if a Category needs to query for
     * what resources may be available on the device (for example, querying permissions).
     */
    abstract fun getThumbnail(context: Context?): Asset?

    /**
     * Returns whether this category allows the user to pick custom photos via Android's photo
     * picker.
     */
    open fun supportsCustomPhotos(): Boolean {
        return false
    }

    /** Returns whether this category is or contains third-party wallpapers */
    open fun supportsThirdParty(): Boolean {
        return false
    }

    /**
     * Returns whether this Category contains or represents a third party wallpaper with the given
     * packageName (this only makes sense if #supportsThirdParty() returns true).
     */
    open fun containsThirdParty(packageName: String?): Boolean {
        return false
    }

    /** Returns whether this category supports content that can be added or removed dynamically. */
    open fun supportsWallpaperSetUpdates(): Boolean {
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Category) return false
        if (other === this) return true
        return TextUtils.equals(collectionId, other.collectionId)
    }

    override fun hashCode(): Int {
        return collectionId.hashCode()
    }
}
