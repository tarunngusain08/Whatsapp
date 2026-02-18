package com.whatsappclone.feature.auth.data

import android.content.SharedPreferences
import android.util.Log
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.core.network.api.AuthApi
import com.whatsappclone.core.network.model.dto.AuthTokenResponse
import com.whatsappclone.core.network.model.dto.SendOtpRequest
import com.whatsappclone.core.network.model.dto.SendOtpResponse
import com.whatsappclone.core.network.model.dto.UserDto
import com.whatsappclone.core.network.model.dto.VerifyOtpRequest
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.network.model.safeApiCallUnit
import com.whatsappclone.core.network.token.DeviceTokenManager
import com.whatsappclone.core.network.token.TokenManager
import javax.inject.Inject
import javax.inject.Named

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val deviceTokenManager: DeviceTokenManager,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) : AuthRepository {

    override suspend fun sendOtp(phone: String): AppResult<SendOtpResponse> {
        return safeApiCall { authApi.sendOtp(SendOtpRequest(phone)) }
    }

    override suspend fun verifyOtp(phone: String, otp: String): AppResult<AuthTokenResponse> {
        val result = safeApiCall { authApi.verifyOtp(VerifyOtpRequest(phone, otp)) }

        if (result is AppResult.Success) {
            try {
                val data = result.data
                tokenManager.saveTokens(data.accessToken, data.refreshToken)
                userDao.upsert(data.user.toEntity())
                encryptedPrefs.edit()
                    .putString(KEY_CURRENT_USER_ID, data.user.id)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist auth state after login", e)
            }
        }

        return result
    }

    override suspend fun logout(): AppResult<Unit> {
        // Unregister the FCM device token BEFORE clearing auth tokens,
        // so the backend DELETE request is still authenticated.
        try {
            deviceTokenManager.unregisterFromBackend()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister device token during logout", e)
        }

        val result = safeApiCallUnit { authApi.logout() }
        tokenManager.clearTokens()
        encryptedPrefs.edit().remove(KEY_CURRENT_USER_ID).apply()
        return result
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    private fun UserDto.toEntity(): UserEntity = UserEntity(
        id = id,
        phone = phone,
        displayName = displayName,
        statusText = statusText,
        avatarUrl = avatarUrl,
        isOnline = isOnline ?: false,
        lastSeen = null,
        isBlocked = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
