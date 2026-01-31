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
import android.content.Context
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

private data class ConnectionKey(
    val packageName: String,
    val serviceName: String,
    val descriptionId: String?,
    val destinationFlag: Int?, // Null if forceSingleEngine is true
    val displaySize: String?, // Null if forceSingleEngine is true
)

/**
 * Handles connecting the rendering of live wallpapers.
 *
 * [LiveWallpaperConnectionUtils] can only be used by refactor_wallpaper_preview_screen_flag.
 */
@ActivityRetainedScoped
class LiveWallpaperConnectionUtils
@Inject
constructor(@ApplicationContext private val context: Context) {

    private val connectionMap: MutableMap<ConnectionKey, LiveWallpaperConnection> = mutableMapOf()
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
     * Establishes or retrieves a connection to a live wallpaper engine.
     *
     * This method manages the lifecycle of the wallpaper connection. It uses a cache to reuse
     * existing engines when possible based on the [forceSingleEngine] flag. This call is
     * thread-safe and suspends while the service is being bound and the engine created.
     *
     * @param onEngineReady A callback triggered when the [IWallpaperEngine] is ready. Note: This is
     *   invoked even if a cached engine is returned (when [forceSingleEngine] is true).
     * @param onWallpaperColorsChanged Callback for engine-level color updates.
     * @return A [LiveWallpaperConnection] representing the active binding and engine.
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
    ): LiveWallpaperConnection {
        val connectionKey: ConnectionKey =
            getConnectionKey(
                forceSingleEngine = forceSingleEngine,
                wallpaperModel = wallpaperModel,
                destinationFlag = destinationFlag,
                engineDisplaySize = engineDisplaySize,
            )
        return mutex.withLock {
            val existingConnection: LiveWallpaperConnection? = connectionMap[connectionKey]
            if (existingConnection != null) {
                val existingEngine: IWallpaperEngine? = existingConnection.wallpaperEngine.get()
                if (existingEngine != null && existingEngine.asBinder().isBinderAlive) {
                    onEngineReady.invoke(existingEngine)
                    return@withLock existingConnection // Found it, return immediately
                } else {
                    // Clear the "dead" connection when its underlying engine is no longer alive.
                    existingConnection.disconnect(context)
                    connectionMap.remove(connectionKey)
                }
            }

            val newConnection =
                createConnection(
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
            connectionMap[connectionKey] = newConnection
            return@withLock newConnection
        }
    }

    /**
     * Terminates all active [LiveWallpaperConnection]s and clears the connection cache. This must
     * be called during teardown (e.g., in `onDestroy`) to prevent memory leaks and ensure that
     * external wallpaper service bindings are released.
     */
    suspend fun disconnectAll() {
        mutex.withLock {
            connectionMap.values.forEach { connection -> connection.disconnect(context) }
            connectionMap.clear()
        }
    }

    private suspend fun createConnection(
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
    ): LiveWallpaperConnection {
        // Bind wallpaper service
        val (serviceConnection, wallpaperService) =
            LiveWallpaperServiceBinder.bindWallpaperService(
                context = context,
                wallpaperModel = wallpaperModel,
            )

        // Create wallpaper engine
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

        return LiveWallpaperConnection(
            context = context,
            wallpaperEngine = WeakReference(engine),
            serviceConnection = WeakReference(serviceConnection),
            wallpaperService = WeakReference(wallpaperService),
            windowToken = WeakReference(windowToken),
        )
    }

    /**
     * Generates a unique key for a [LiveWallpaperConnection] instance.
     *
     * @param forceSingleEngine If true, creates a global key for the wallpaper, ensuring only one
     *   connection (engine) is instantiated regardless of where it is displayed. If false, the key
     *   includes [destinationFlag] and [engineDisplaySize] to allow separate engine instances for
     *   different display contexts (e.g., home vs. lock screen).
     * @return A [ConnectionKey] used to identify and cache [LiveWallpaperConnection].
     */
    private fun getConnectionKey(
        forceSingleEngine: Boolean,
        wallpaperModel: LiveWallpaperModel,
        destinationFlag: Int,
        engineDisplaySize: Point,
    ): ConnectionKey {
        val info: WallpaperInfo = wallpaperModel.liveWallpaperData.systemWallpaperInfo
        val descId: String? = wallpaperModel.liveWallpaperData.description.id
        return if (forceSingleEngine)
            ConnectionKey(
                packageName = info.packageName,
                serviceName = info.serviceName,
                descriptionId = descId,
                destinationFlag = null,
                displaySize = null,
            )
        else
            ConnectionKey(
                packageName = info.packageName,
                serviceName = info.serviceName,
                descriptionId = descId,
                destinationFlag = destinationFlag,
                displaySize = "${engineDisplaySize.x}x${engineDisplaySize.y}",
            )
    }
}
