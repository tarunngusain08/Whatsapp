package com.whatsappclone.core.network.token

/**
 * Abstraction for managing push notification device tokens.
 *
 * This interface lives in `:core:network` so that feature modules
 * (like `:feature:auth`) can call [unregisterFromBackend] during
 * logout without depending on the `:app` module where the concrete
 * Firebase implementation lives.
 */
interface DeviceTokenManager {

    /** Registers the current device token with the backend. */
    suspend fun registerWithBackend()

    /** Unregisters the device token from the backend (call on logout). */
    suspend fun unregisterFromBackend()
}
