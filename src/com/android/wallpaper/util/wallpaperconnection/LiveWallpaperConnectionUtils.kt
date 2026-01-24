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

import android.app.WallpaperColors
import android.app.WallpaperInfo
import android.app.wallpaper.WallpaperDescription
import android.content.Context
import android.content.ServiceConnection
import android.graphics.Point
import android.os.IBinder
import android.service.wallpaper.IWallpaperEngine
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import com.android.wallpaper.util.toDescription
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles connecting the rendering of live wallpapers.
 *
 * [LiveWallpaperConnectionUtils] can only be used by refactor_wallpaper_preview_screen_flag.
 */
@ActivityRetainedScoped
class LiveWallpaperConnectionUtils @Inject constructor(@ApplicationContext context: Context) {

    // Note that we only need to use mutex and cache the engine map when forceSingleEngine is true.
    // Otherwise, we always create a new engine when connect.
    // TODO (b/423956081): Make sure that previous engines are properly disconnected when not needed
    //                     in both cases when forceSingleEngine is true / false.
    private val liveWallpaperEngines: MutableMap<String, IWallpaperEngine> = mutableMapOf()
    private val mutex = Mutex()

    init {
        if (!BaseFlags.get(context).isRefactorWallpaperPreviewScreenEnabled()) {
            throw IllegalStateException(
                "LiveWallpaperConnectionUtils can only be used when " +
                    "refactor_wallpaper_preview_screen_flag is turned on."
            )
        }
    }

    /**
     * Connect a live wallpaper from remote.
     *
     * @param onEngineReady Note that onEngineReady will still be called for the case of returning
     *   an already-created engine when forceSingleEngine is true.
     */
    suspend fun connect(
        context: Context,
        wallpaperModel: LiveWallpaperModel,
        forceSingleEngine: Boolean,
        destinationFlag: Int,
        engineDisplaySize: Point,
        windowToken: IBinder,
        displayId: Int,
        whichPreview: WhichPreview,
        onEngineReady: (engine: IWallpaperEngine) -> Unit,
        onWallpaperColorsChanged:
            (colors: WallpaperColors?, displayId: Int, persistedColors: WallpaperColors?) -> Unit,
    ): IWallpaperEngine {
        val engineKey: String =
            getEngineKey(
                forceSingleEngine = forceSingleEngine,
                wallpaperModel = wallpaperModel,
                destinationFlag = destinationFlag,
                engineDisplaySize = engineDisplaySize,
            )
        return mutex.withLock {
            val existingEngine = liveWallpaperEngines[engineKey]
            if (existingEngine != null) {
                onEngineReady.invoke(existingEngine)
                return@withLock existingEngine // Found it, return immediately
            }

            val newEngine =
                bindWallpaperServiceAndCreateEngine(
                    context = context,
                    wallpaperModel = wallpaperModel,
                    destinationFlag = destinationFlag,
                    displaySize = engineDisplaySize,
                    windowToken = windowToken,
                    displayId = displayId,
                    whichPreview = whichPreview,
                    onEngineCreated = onEngineReady,
                    onWallpaperColorsChanged = onWallpaperColorsChanged,
                )

            liveWallpaperEngines[engineKey] = newEngine

            return@withLock newEngine
        }
    }

    private suspend fun bindWallpaperServiceAndCreateEngine(
        context: Context,
        wallpaperModel: LiveWallpaperModel,
        destinationFlag: Int,
        displaySize: Point,
        windowToken: IBinder,
        displayId: Int,
        whichPreview: WhichPreview,
        onEngineCreated: (engine: IWallpaperEngine) -> Unit,
        onWallpaperColorsChanged:
            (colors: WallpaperColors?, displayId: Int, persistedColors: WallpaperColors?) -> Unit,
    ): IWallpaperEngine {
        val (serviceConnection, wallpaperService) =
            LiveWallpaperServiceBinder.bindWallpaperService(
                context = context,
                wallpaperModel = wallpaperModel,
            )

        val engine: IWallpaperEngine =
            LiveWallpaperEngineCreator.createEngine(
                context = context,
                wallpaperService = wallpaperService,
                destinationFlag = destinationFlag,
                description = wallpaperModel.toDescription(),
                displaySize = displaySize,
                windowToken = windowToken,
                displayId = displayId, // TODO(b/423956081): give a proper ID here
                whichPreview = whichPreview,
                onWallpaperColorsChanged = onWallpaperColorsChanged,
            )
        onEngineCreated.invoke(engine)

        val connection =
            LiveWallpaperConnections(
                context = context,
                wallpaperEngine = WeakReference(engine),
                serviceConnection = WeakReference(serviceConnection),
                wallpaperService = WeakReference(wallpaperService),
                windowToken = WeakReference(windowToken),
            )

        // Set up death listeners for service and engine
        val disconnectAction = { connection.disconnect(context) }
        (serviceConnection as? WallpaperServiceConnection)?.deadConnectionListener =
            object : WallpaperServiceConnection.DeadConnectionListener {
                override fun onConnectionDead(serviceConnection: ServiceConnection) {
                    disconnectAction.invoke()
                }
            }
        wallpaperService.asBinder()?.linkToDeath(disconnectAction, 0)
        engine.asBinder()?.linkToDeath(disconnectAction, 0)

        return engine
    }

    /**
     * Generates a unique key for an engine instance.
     *
     * @param forceSingleEngine If true, creates a global key for the wallpaper, ensuring only one
     *   engine is instantiated regardless of where it is displayed. If false, the key includes
     *   [destinationFlag] and [engineDisplaySize] to allow separate engine instances for different
     *   display contexts (e.g., home vs. lock screen).
     * @return A unique string key used to identify and cache the wallpaper engine.
     */
    private fun getEngineKey(
        forceSingleEngine: Boolean,
        wallpaperModel: LiveWallpaperModel,
        destinationFlag: Int,
        engineDisplaySize: Point,
    ): String {
        val wallpaperInfo: WallpaperInfo = wallpaperModel.liveWallpaperData.systemWallpaperInfo
        val description: WallpaperDescription = wallpaperModel.liveWallpaperData.description
        val engineDisplaySizeString = "${engineDisplaySize.x}x${engineDisplaySize.y}"
        return if (forceSingleEngine)
            "${wallpaperInfo.packageName}:${wallpaperInfo.serviceName}:${description.id}"
        else
            "${wallpaperInfo.packageName}:${wallpaperInfo.serviceName}:${description.id}:${destinationFlag}:$engineDisplaySizeString"
    }
}
