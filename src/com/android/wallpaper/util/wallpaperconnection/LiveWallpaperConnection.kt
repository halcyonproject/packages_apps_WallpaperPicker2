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

package com.android.wallpaper.util.wallpaperconnection

import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.service.wallpaper.IWallpaperEngine
import android.service.wallpaper.IWallpaperService
import android.util.Log
import com.android.wallpaper.config.BaseFlags
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A wrapper for [IWallpaperEngine], [ServiceConnection], and [IWallpaperService] references created
 * during live wallpaper rendering.
 *
 * This class ensures that these components can be properly cleaned up and disconnected when no
 * longer needed. Each [LiveWallpaperConnection] instance should maintain a 1:1 mapping with its
 * respective engine, connection, and service.
 *
 * Note that this class is only intended for use when the wallpaper preview refactor flag is
 * enabled. Use should be gated by [BaseFlags.isRefactorWallpaperPreviewScreenEnabled].
 */
data class LiveWallpaperConnection(
    val context: Context,
    val wallpaperEngine: WeakReference<IWallpaperEngine>,
    val serviceConnection: WeakReference<ServiceConnection>,
    val wallpaperService: WeakReference<IWallpaperService>,
    val windowToken: WeakReference<IBinder>,
) {
    private val disconnected = AtomicBoolean(false)

    init {
        if (!BaseFlags.get(context).isRefactorWallpaperPreviewScreenEnabled()) {
            throw IllegalStateException(
                "LiveWallpaperConnection can only be used when " +
                    "refactor_wallpaper_preview_screen_flag is turned on."
            )
        }
    }

    fun disconnect(context: Context) {
        if (disconnected.compareAndSet(false, true)) {
            wallpaperEngine.get()?.apply {
                try {
                    destroy()
                } catch (e: RemoteException) {
                    Log.w(
                        TAG,
                        "Error destroying wallpaper engine. It can be that engine is " +
                            "already destroyed.",
                        e,
                    )
                }
            }
            wallpaperEngine.clear()
            windowToken.get()?.let { windowToken ->
                wallpaperService.get()?.let { service ->
                    try {
                        service.detach(windowToken)
                    } catch (e: RemoteException) {
                        Log.w(
                            TAG,
                            "Error disconnecting wallpaper service. It can be that " +
                                "service has already detached.",
                            e,
                        )
                    }
                }
            }
            windowToken.clear()
            serviceConnection.get()?.let {
                try {
                    context.unbindService(it)
                } catch (e: RemoteException) {
                    Log.w(
                        TAG,
                        "Error unbinding service connection. It can be that service " +
                            "connection has already unbound.",
                        e,
                    )
                }
            }
            serviceConnection.clear()
        }
    }

    companion object {
        const val TAG = "LiveWallpaperConnection"
    }
}
