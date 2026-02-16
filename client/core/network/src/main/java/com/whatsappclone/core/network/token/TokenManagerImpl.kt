package com.whatsappclone.core.network.token

import android.content.SharedPreferences
import com.whatsappclone.core.network.api.AuthApi
import com.whatsappclone.core.network.model.dto.RefreshRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named

class TokenManagerImpl @Inject constructor(
    @Named("encrypted") private val encryptedPrefs: SharedPreferences,
    private val authApi: dagger.Lazy<AuthApi>
) : TokenManager {

    private val mutex = Mutex()

    override fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    override fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    override suspend fun refreshToken(): Boolean {
        return mutex.withLock {
            val currentRefreshToken = getRefreshToken() ?: return@withLock false
            try {
                val response = authApi.get().refreshToken(
                    RefreshRequest(refreshToken = currentRefreshToken)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.data != null) {
                        saveTokens(
                            accessToken = body.data!!.accessToken,
                            refreshToken = body.data!!.refreshToken
                        )
                        return@withLock true
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    override fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
