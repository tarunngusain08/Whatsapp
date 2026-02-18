package com.whatsappclone.app.lifecycle

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.whatsappclone.core.network.token.TokenManager
import com.whatsappclone.app.data.websocket.SyncOnReconnectManager
import com.whatsappclone.app.data.websocket.WsEventRouter
import com.whatsappclone.core.network.websocket.WebSocketManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages WebSocket connection based on app lifecycle and auth state.
 *
 * - Connects when the user is authenticated and the app is in the foreground.
 * - Starts the [WsEventRouter] and [SyncOnReconnectManager] alongside the connection.
 * - Keeps the connection alive when the app is backgrounded (server-side timeout handles
 *   eventual disconnect) — a short grace period before disconnect can be added in Phase 2.
 * - Provides [stop] for explicit teardown on logout.
 */
@Singleton
class WsLifecycleManager @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val wsEventRouter: WsEventRouter,
    private val syncOnReconnectManager: SyncOnReconnectManager,
    private val tokenManager: TokenManager
) : DefaultLifecycleObserver {

    private var isStarted = false

    /**
     * Starts the WebSocket pipeline only if the user is logged in and it hasn't
     * been started already.
     */
    fun startIfAuthenticated() {
        try {
            if (!tokenManager.isLoggedIn() || isStarted) return
            isStarted = true
            webSocketManager.connect()
            wsEventRouter.start()
            syncOnReconnectManager.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebSocket pipeline", e)
        }
    }

    /**
     * Tears down the WebSocket connection. Call on logout or when the session expires.
     */
    fun stop() {
        try {
            isStarted = false
            webSocketManager.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop WebSocket pipeline", e)
        }
    }

    // ── Lifecycle callbacks ──────────────────────────────────────────────

    override fun onStart(owner: LifecycleOwner) {
        try {
            if (tokenManager.isLoggedIn()) {
                startIfAuthenticated()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStart lifecycle callback", e)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // Keep the WebSocket connected when backgrounded.
        // The server-side idle timeout will eventually close the connection.
        // A future enhancement could add a delayed disconnect here.
    }

    companion object {
        private const val TAG = "WsLifecycleManager"
    }
}
