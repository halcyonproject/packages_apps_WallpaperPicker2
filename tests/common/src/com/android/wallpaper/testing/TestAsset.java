/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.wallpaper.testing;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.wallpaper.asset.Asset;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Test implementation of Asset which blocks on Bitmap decoding operations.
 */
public final class TestAsset extends Asset {
    private static final String TAG = "TestAsset";

    private Bitmap mBitmap;
    private final boolean mIsCorrupt;

    /**
     * Constructs an asset underpinned by a 1x1 bitmap uniquely identifiable by the given pixel
     * color.
     *
     * @param pixelColor Color of the asset's single pixel.
     * @param isCorrupt Whether or not the asset is corrupt and fails to validly decode bitmaps and
     * dimensions.
     */
    public TestAsset(int pixelColor, boolean isCorrupt) {
        this(pixelColor, isCorrupt, 1, 1);
    }

    /**
     * Constructs an asset underpinned by a width x height bitmap uniquely identifiable by the given
     * pixel color.
     *
     * @param pixelColor Color of the asset's single pixel.
     * @param isCorrupt Whether or not the asset is corrupt and fails to validly decode bitmaps and
     * dimensions.
     * @param width Width of asset image
     * @param height Height of asset image
     */
    public TestAsset(int pixelColor, boolean isCorrupt, int width, int height) {
        mIsCorrupt = isCorrupt;

        if (!mIsCorrupt) {
            mBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            mBitmap.setPixel(0, 0, pixelColor);
        } else {
            mBitmap = null;
        }
    }

    @Override
    public void decodeBitmap(BitmapReceiver receiver) {
        Handler.getMain().post(() ->
                receiver.onBitmapDecoded(mBitmap));
    }

    @Override
    public void decodeBitmap(int targetWidth, int targetHeight, boolean useHardwareBitmapIfPossible,
            BitmapReceiver receiver) {
        Handler.getMain().post(() ->
                receiver.onBitmapDecoded(mBitmap));
    }

    @Override
    public void decodeBitmapRegion(Rect unused, int targetWidth, int targetHeight,
            boolean shouldAdjustForRtl, BitmapReceiver receiver) {
        Handler.getMain().post(() ->
                receiver.onBitmapDecoded(mBitmap));
    }

    @Override
    public void decodeRawDimensions(Activity unused, DimensionsReceiver receiver) {
        Handler.getMain().post(() ->
                receiver.onDimensionsDecoded(
                        mIsCorrupt ? null : new Point(mBitmap.getWidth(), mBitmap.getHeight())));
    }

    @Override
    public boolean supportsTiling() {
        return false;
    }

    @Override
    public void loadDrawableWithTransition(
            Context context,
            ImageView imageView,
            int transitionDurationMillis,
            @Nullable DrawableLoadedListener drawableLoadedListener,
            int placeholderColor, @Nullable PermissionErrorListener permissionErrorListener) {
        if (drawableLoadedListener != null) {
            drawableLoadedListener.onDrawableLoaded();
        }
    }

    /**
     * Returns the bitmap synchronously. Convenience method for tests.
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    public Bitmap getLowResBitmap(Context context) {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    @Override
    public void copy(File dest) {
        try (FileOutputStream ofs = new FileOutputStream(dest)) {
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, ofs);
        } catch (IOException e) {
            Log.e(TAG, "Error writing test asset", e);
            throw new RuntimeException(e);
        }
    }
}
