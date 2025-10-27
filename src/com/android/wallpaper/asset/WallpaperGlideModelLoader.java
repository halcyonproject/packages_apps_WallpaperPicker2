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
package com.android.wallpaper.asset;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

/**
 * Custom Glide {@link ModelLoader} which can load {@link Drawable} objects from
 * {@link WallpaperGlideModel} objects.
 */
public class WallpaperGlideModelLoader implements ModelLoader<WallpaperGlideModel, Drawable> {

    @Override
    public boolean handles(WallpaperGlideModel wallpaperGlideModel) {
        return true;
    }

    @Nullable
    @Override
    public LoadData<Drawable> buildLoadData(WallpaperGlideModel wallpaperGlideModel, int width,
            int height,
            Options options) {
        return new LoadData<>(wallpaperGlideModel.getKey(),
                new WallpaperFetcher(wallpaperGlideModel, width, height));
    }

    /**
     * Factory that constructs {@link WallpaperGlideModelLoader} instances.
     */
    public static class WallpaperGlideModelLoaderFactory
            implements ModelLoaderFactory<WallpaperGlideModel, Drawable> {
        public WallpaperGlideModelLoaderFactory() {
        }

        @Override
        public ModelLoader<WallpaperGlideModel, Drawable> build(
                MultiModelLoaderFactory multiFactory) {
            return new WallpaperGlideModelLoader();
        }

        @Override
        public void teardown() {
            // no-op
        }
    }

    /**
     * Fetcher class for fetching wallpaper image data from a {@link WallpaperGlideModel}.
     */
    private static class WallpaperFetcher implements DataFetcher<Drawable> {

        private WallpaperGlideModel mWallpaperGlideModel;
        private int mWidth;
        private int mHeight;

        WallpaperFetcher(WallpaperGlideModel wallpaperGlideModel, int width, int height) {
            mWallpaperGlideModel = wallpaperGlideModel;
            mWidth = width;
            mHeight = height;
        }

        @Override
        public void loadData(Priority priority, DataCallback<? super Drawable> callback) {
            callback.onDataReady(mWallpaperGlideModel.getDrawable(mWidth, mHeight));
        }

        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }

        @Override
        public void cancel() {
            // no-op
        }

        @Override
        public void cleanup() {
            // no-op
        }

        @Override
        public Class<Drawable> getDataClass() {
            return Drawable.class;
        }
    }
}
