/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.wallpaper.picker;

import android.app.WallpaperManager;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.service.wallpaper.WallpaperService;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.wallpaper.R;
import com.android.wallpaper.model.Category;
import com.android.wallpaper.model.CategoryProvider;
import com.android.wallpaper.model.CategoryReceiver;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.PackageStatusNotifier;
import com.android.wallpaper.module.PackageStatusNotifier.PackageStatus;
import com.android.wallpaper.module.WallpaperPreferences;
import com.android.wallpaper.picker.WallpaperDisabledFragment.WallpaperSupportLevel;

public class WallpaperPickerDelegate {

    private final FragmentActivity mActivity;
    private final WallpapersUiContainer mContainer;
    public static final int SHOW_CATEGORY_REQUEST_CODE = 0;
    public static final int PREVIEW_WALLPAPER_REQUEST_CODE = 1;
    public static final int PREVIEW_LIVE_WALLPAPER_REQUEST_CODE = 4;
    private WallpaperPreferences mPreferences;
    private PackageStatusNotifier mPackageStatusNotifier;
    private PackageStatusNotifier.Listener mLiveWallpaperStatusListener;
    private PackageStatusNotifier.Listener mThirdPartyStatusListener;
    private PackageStatusNotifier.Listener mDownloadableWallpaperStatusListener;
    private String mDownloadableIntentAction;
    private CategoryProvider mCategoryProvider;

    public WallpaperPickerDelegate(WallpapersUiContainer container, FragmentActivity activity,
            Injector injector) {
        mContainer = container;
        mActivity = activity;
        mCategoryProvider = injector.getCategoryProvider(activity);
        mPreferences = injector.getPreferences(activity);

        mPackageStatusNotifier = injector.getPackageStatusNotifier(activity);

        mDownloadableIntentAction = injector.getDownloadableIntentAction();
    }

    public void initialize(boolean forceCategoryRefresh) {
            populateCategories(forceCategoryRefresh);
            mLiveWallpaperStatusListener = this::updateLiveWallpapersCategories;
            mThirdPartyStatusListener = this::updateThirdPartyCategories;
            mPackageStatusNotifier.addListener(
                    mLiveWallpaperStatusListener,
                    WallpaperService.SERVICE_INTERFACE);
            mPackageStatusNotifier.addListener(mThirdPartyStatusListener,
                    Intent.ACTION_SET_WALLPAPER);
            if (mDownloadableIntentAction != null) {
                mDownloadableWallpaperStatusListener = (packageName, status) -> {
                    if (status != PackageStatusNotifier.PackageStatus.REMOVED) {
                        populateCategories(/* forceRefresh= */ true);
                    }
                };
                mPackageStatusNotifier.addListener(
                        mDownloadableWallpaperStatusListener, mDownloadableIntentAction);
            }
    }

    private void updateThirdPartyCategories(String packageName, @PackageStatus int status) {
        if (status == PackageStatus.ADDED) {
            mCategoryProvider.fetchCategories(new CategoryReceiver() {
                @Override
                public void onCategoryReceived(Category category) {
                    if (category.supportsThirdParty() && category.containsThirdParty(packageName)) {
                        addCategory(category, false);
                    }
                }

                @Override
                public void doneFetchingCategories() {
                    // Do nothing here.
                }
            }, true);
        } else if (status == PackageStatus.REMOVED) {
            Category oldCategory = findThirdPartyCategory(packageName);
            if (oldCategory != null) {
                mCategoryProvider.fetchCategories(new CategoryReceiver() {
                    @Override
                    public void onCategoryReceived(Category category) {
                        // Do nothing here
                    }

                    @Override
                    public void doneFetchingCategories() {
                        removeCategory(oldCategory);
                    }
                }, true);
            }
        } else {
            // CHANGED package, let's reload all categories as we could have more or fewer now
            populateCategories(/* forceRefresh= */ true);
        }
    }

    private Category findThirdPartyCategory(String packageName) {
        int size = mCategoryProvider.getSize();
        for (int i = 0; i < size; i++) {
            Category category = mCategoryProvider.getCategory(i);
            if (category.supportsThirdParty() && category.containsThirdParty(packageName)) {
                return category;
            }
        }
        return null;
    }

    private void updateLiveWallpapersCategories(String packageName,
            @PackageStatus int status) {
        String liveWallpaperCollectionId = mActivity.getString(
                R.string.live_wallpaper_collection_id);
        Category oldLiveWallpapersCategory = mCategoryProvider.getCategory(
                liveWallpaperCollectionId);
        if (status == PackageStatus.REMOVED
                && (oldLiveWallpapersCategory == null
                || !oldLiveWallpapersCategory.containsThirdParty(packageName))) {
            // If we're removing a wallpaper and the live category didn't contain it already,
            // there's nothing to do.
            return;
        }
        mCategoryProvider.fetchCategories(new CategoryReceiver() {
            @Override
            public void onCategoryReceived(Category category) {
                // Do nothing here
            }

            @Override
            public void doneFetchingCategories() {
                Category liveWallpapersCategory =
                        mCategoryProvider.getCategory(liveWallpaperCollectionId);
                if (liveWallpapersCategory == null) {
                    // There are no more 3rd party live wallpapers, so the Category is gone.
                    removeCategory(oldLiveWallpapersCategory);
                } else {
                    if (oldLiveWallpapersCategory != null) {
                        updateCategory(liveWallpapersCategory);
                    } else {
                        addCategory(liveWallpapersCategory, false);
                    }
                }
            }
        }, true);
    }

    /**
     * Populates the categories appropriately.
     *
     * @param forceRefresh        Whether to force a refresh of categories from the
     *                            CategoryProvider. True if
     *                            on first launch.
     */
    public void populateCategories(boolean forceRefresh) {

        final CategorySelectorFragment categorySelectorFragment = getCategorySelectorFragment();

        if (forceRefresh && categorySelectorFragment != null) {
            categorySelectorFragment.clearCategories();
        }

        mCategoryProvider.fetchCategories(new CategoryReceiver() {
            @Override
            public void onCategoryReceived(Category category) {
                addCategory(category, true);
            }

            @Override
            public void doneFetchingCategories() {
                notifyDoneFetchingCategories();
            }
        }, forceRefresh);
    }

    private void notifyDoneFetchingCategories() {
        CategorySelectorFragment categorySelectorFragment = getCategorySelectorFragment();
        if (categorySelectorFragment != null) {
            categorySelectorFragment.doneFetchingCategories();
        }
    }

    public void addCategory(Category category, boolean fetchingAll) {
        CategorySelectorFragment categorySelectorFragment = getCategorySelectorFragment();
        if (categorySelectorFragment != null) {
            categorySelectorFragment.addCategory(category, fetchingAll);
        }
    }

    public void removeCategory(Category category) {
        CategorySelectorFragment categorySelectorFragment = getCategorySelectorFragment();
        if (categorySelectorFragment != null) {
            categorySelectorFragment.removeCategory(category);
        }
    }

    public void updateCategory(Category category) {
        CategorySelectorFragment categorySelectorFragment = getCategorySelectorFragment();
        if (categorySelectorFragment != null) {
            categorySelectorFragment.updateCategory(category);
        }
    }

    @Nullable
    private CategorySelectorFragment getCategorySelectorFragment() {
        return mContainer.getCategorySelectorFragment();
    }

    /**
     * Shows the picker activity for the given category.
     */
    public void show(String collectionId) {
        Category category = findCategoryForCollectionId(collectionId);
        if (category == null) {
            return;
        }
        category.show(mActivity, SHOW_CATEGORY_REQUEST_CODE);
    }

    @Nullable
    public Category findCategoryForCollectionId(String collectionId) {
        return mCategoryProvider.getCategory(collectionId);
    }

    @WallpaperSupportLevel
    public int getWallpaperSupportLevel() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mActivity);

        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            if (wallpaperManager.isWallpaperSupported()) {
                return wallpaperManager.isSetWallpaperAllowed()
                        ? WallpaperDisabledFragment.SUPPORTED_CAN_SET
                        : WallpaperDisabledFragment.NOT_SUPPORTED_BLOCKED_BY_ADMIN;
            }
            return WallpaperDisabledFragment.NOT_SUPPORTED_BY_DEVICE;
        } else if (VERSION.SDK_INT >= VERSION_CODES.M) {
            return wallpaperManager.isWallpaperSupported()
                    ? WallpaperDisabledFragment.SUPPORTED_CAN_SET
                    : WallpaperDisabledFragment.NOT_SUPPORTED_BY_DEVICE;
        } else {
            boolean isSupported = WallpaperManager.getInstance(mActivity.getApplicationContext())
                    .getDrawable() != null;
            wallpaperManager.forgetLoadedWallpaper();
            return isSupported ? WallpaperDisabledFragment.SUPPORTED_CAN_SET
                    : WallpaperDisabledFragment.NOT_SUPPORTED_BY_DEVICE;
        }
    }

    public WallpaperPreferences getPreferences() {
        return mPreferences;
    }

    /**
     * Call when the owner activity is destroyed to clean up listeners.
     */
    public void cleanUp() {
        if (mPackageStatusNotifier != null) {
            mPackageStatusNotifier.removeListener(mLiveWallpaperStatusListener);
            mPackageStatusNotifier.removeListener(mThirdPartyStatusListener);
            mPackageStatusNotifier.removeListener(mDownloadableWallpaperStatusListener);
        }
    }
}
