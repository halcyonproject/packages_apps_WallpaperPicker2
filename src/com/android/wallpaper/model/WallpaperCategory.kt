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
import android.content.res.Resources
import android.util.AttributeSet
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.asset.ResourceAsset

/** Default category for a collection of WallpaperInfo objects. */
open class WallpaperCategory : Category {
    @JvmField protected val wallpapersLock: Any

    /**
     * Returns the mutable list of wallpapers backed by this WallpaperCategory. All reads and writes
     * on the returned list must be synchronized with `mWallpapersLock`.
     */
    val wallpapers: List<WallpaperInfo>?
    var thumbAsset: Asset? = null
        private set

    var featuredThumbnailIndex: Int = 0
        private set

    constructor(
        title: String,
        collectionId: String,
        wallpapers: List<WallpaperInfo>?,
        priority: Int,
        isDownloadable: Boolean,
        downloadComponent: String?,
    ) : this(title, collectionId, 0, wallpapers, priority, isDownloadable, downloadComponent)

    constructor(
        title: String,
        collectionId: String,
        wallpapers: List<WallpaperInfo>?,
        priority: Int,
    ) : this(title, collectionId, 0, wallpapers, priority, false, null)

    @JvmOverloads
    constructor(
        title: String,
        collectionId: String,
        featuredThumbnailIndex: Int,
        wallpapers: List<WallpaperInfo>?,
        priority: Int,
        isDownloadable: Boolean = false,
        downloadComponent: String? = null,
    ) : super(title, collectionId, priority, isDownloadable, downloadComponent) {
        this.wallpapers = wallpapers
        wallpapersLock = Any()
        this.featuredThumbnailIndex = featuredThumbnailIndex
    }

    constructor(
        title: String,
        collectionId: String,
        thumbAsset: Asset?,
        wallpapers: List<WallpaperInfo>?,
        priority: Int,
    ) : super(title, collectionId, priority, false, null) {
        this.wallpapers = wallpapers
        wallpapersLock = Any()
        this.thumbAsset = thumbAsset
    }

    constructor(
        title: String,
        collectionId: String,
        thumbAsset: Asset?,
        priority: Int,
    ) : this(title, collectionId, thumbAsset, null, priority)

    /**
     * Fetches wallpapers for this category and passes them to the receiver. Subclasses may use a
     * context to fetch wallpaper info.
     */
    open fun fetchWallpapers(unused: Context?, receiver: WallpaperReceiver, forceReload: Boolean) {
        // Perform a shallow clone so as not to pass the reference to the list along to clients.
        receiver.onWallpapersReceived(wallpapers)
    }

    override fun show(srcActivity: Activity?, requestCode: Int) {
        // No op
    }

    override val isEnumerable: Boolean
        get() = true

    override val isSingleWallpaperCategory: Boolean
        get() = wallpapers != null && wallpapers.size == 1

    override val singleWallpaper: WallpaperInfo?
        get() = if (isSingleWallpaperCategory) wallpapers!![0] else null

    val unmodifiableWallpapers: List<WallpaperInfo>
        /** Returns an unmodifiable view the list of wallpapers in this WallpaperCategory. */
        get() = wallpapers ?: emptyList()

    override fun getThumbnail(context: Context?): Asset? {
        synchronized(wallpapersLock) {
            if (thumbAsset == null && !wallpapers.isNullOrEmpty()) {
                thumbAsset = wallpapers[featuredThumbnailIndex].getThumbAsset(context)
            }
        }
        return thumbAsset
    }

    override fun supportsThirdParty(): Boolean {
        return false
    }

    override fun containsThirdParty(packageName: String?): Boolean {
        return false
    }

    /** Builder used to construct a [WallpaperCategory] object from an XML's [AttributeSet]. */
    class Builder(private val mPartnerRes: Resources?, attrs: AttributeSet) {
        private val mWallpapers: MutableList<WallpaperInfo> = ArrayList()
        val id: String = attrs.getAttributeValue(null, "id")
        private val mTitle: String
        private var mPriority: Int
        private val mFeaturedId: String?

        @IdRes private val mThumbResId: Int

        init {
            @StringRes val titleResId = attrs.getAttributeResourceValue(null, "title", 0)
            mTitle =
                if (titleResId != 0) {
                    mPartnerRes?.getString(titleResId) ?: ""
                } else {
                    ""
                }
            mFeaturedId = attrs.getAttributeValue(null, "featured")
            mPriority = attrs.getAttributeIntValue(null, "priority", -1)
            mThumbResId = attrs.getAttributeResourceValue(null, "thumbnail", 0)
        }

        /**
         * Add the given [WallpaperInfo] to this category
         *
         * @return this for chaining
         */
        fun addWallpaper(info: WallpaperInfo): Builder {
            mWallpapers.add(info)
            return this
        }

        /**
         * Adds the given list of [WallpaperInfo] to this category
         *
         * @return this for chaining
         */
        fun addWallpapers(wallpapers: List<WallpaperInfo>?): Builder {
            mWallpapers.addAll(wallpapers!!)
            return this
        }

        /**
         * If no priority was parsed from the XML attributes for this category, set the priority to
         * the given value.
         *
         * @return this for chaining
         */
        fun setPriorityIfEmpty(priority: Int): Builder {
            if (mPriority < 0) {
                mPriority = priority
            }
            return this
        }

        /** Build a [WallpaperCategory] with this builder's information */
        fun build(): WallpaperCategory {
            if (mThumbResId != 0) {
                return WallpaperCategory(
                    mTitle,
                    id,
                    ResourceAsset(mPartnerRes, mThumbResId, true),
                    mWallpapers,
                    mPriority,
                )
            } else {
                var featuredIndex = 0
                for (i in mWallpapers.indices) {
                    if (mWallpapers[i].wallpaperId == mFeaturedId) {
                        featuredIndex = i
                        break
                    }
                }
                return WallpaperCategory(mTitle, id, featuredIndex, mWallpapers, mPriority)
            }
        }

        /** Build a [PlaceholderCategory] with this builder's information. */
        fun buildPlaceholder(): Category {
            return PlaceholderCategory(mTitle, id, mPriority)
        }
    }

    companion object {
        const val TAG_NAME: String = "category"
    }
}
