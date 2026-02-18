package com.whatsappclone.app.notification

import android.util.Log
import com.whatsappclone.core.network.api.NotificationApi
import com.whatsappclone.core.network.model.dto.DeviceTokenRequest
import com.whatsappclone.core.network.token.DeviceTokenManager
import com.whatsappclone.core.network.token.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manages the FCM device-token lifecycle:
 *
 * - Retrieves the current FCM token (safely, in case Firebase is not initialized).
 * - Registers/unregisters the token with the backend via [NotificationApi].
 * - Observes token refreshes and re-registers automatically.
 *
 * All Firebase calls are wrapped in try/catch so the app remains fully
 * functional even when `google-services.json` is missing or Firebase
 * has not been initialized.
 */
@Singleton
class FCMTokenManager @Inject constructor(
    private val notificationApi: NotificationApi,
    private val tokenManager: TokenManager,
    @Named("appScope") private val coroutineScope: CoroutineScope
) : DeviceTokenManager {

    @Volatile
    private var cachedToken: String? = null

    /**
     * Safely retrieves the current FCM registration token.
     * Returns `null` if Firebase is not available.
     */
    suspend fun getToken(): String? {
        cachedToken?.let { return it }

        return try {
            val firebaseMessaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            val token = firebaseMessaging.token.await()
            cachedToken = token
            token
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get FCM token (Firebase may not be initialized)", e)
            null
        }
    }

    /**
     * Retrieves the current FCM token and registers it with the backend.
     * No-op if the user is not logged in or Firebase is unavailable.
     */
    override suspend fun registerWithBackend() {
        if (!tokenManager.isLoggedIn()) {
            Log.d(TAG, "Skipping FCM registration — user not logged in")
            return
        }

        val token = getToken()
        if (token == null) {
            Log.w(TAG, "Skipping FCM registration — no token available")
            return
        }

        try {
            val response = notificationApi.registerDevice(
                DeviceTokenRequest(token = token, platform = "android")
            )
            if (response.isSuccessful) {
                Log.d(TAG, "FCM token registered with backend")
            } else {
                Log.w(TAG, "Backend rejected FCM token: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token with backend", e)
        }
    }

    /**
     * Unregisters the current device token from the backend.
     * Call on logout to stop receiving push notifications.
     */
    override suspend fun unregisterFromBackend() {
        val token = cachedToken ?: getToken()
        if (token == null) {
            Log.w(TAG, "Skipping FCM unregistration — no token available")
            return
        }

        try {
            val response = notificationApi.unregisterDevice(token)
            if (response.isSuccessful) {
                Log.d(TAG, "FCM token unregistered from backend")
            } else {
                Log.w(TAG, "Backend rejected FCM unregistration: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister FCM token from backend", e)
        }

        cachedToken = null
    }

    /**
     * Called when FCM issues a new token (e.g. via `onNewToken`).
     * Caches it and re-registers with the backend if the user is logged in.
     */
    fun onNewToken(token: String) {
        cachedToken = token
        if (tokenManager.isLoggedIn()) {
            coroutineScope.launch {
                registerWithBackend()
            }
        }
    }

    /**
     * Starts observing FCM token refreshes. Should be called once at
     * application startup. This is a safety net in addition to the
     * `onNewToken` callback in [WhatsAppFCMService].
     */
    fun observeTokenRefresh() {
        try {
            val firebaseMessaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            firebaseMessaging.token.addOnSuccessListener { token ->
                if (token != cachedToken) {
                    onNewToken(token)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot observe FCM token refresh (Firebase may not be initialized)", e)
        }
    }

    companion object {
        private const val TAG = "FCMTokenManager"
    }
}
