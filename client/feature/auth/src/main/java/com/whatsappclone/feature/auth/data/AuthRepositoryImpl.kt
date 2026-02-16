package com.whatsappclone.feature.auth.data

import android.util.Log
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.core.network.api.AuthApi
import com.whatsappclone.core.network.api.NotificationApi
import com.whatsappclone.core.network.model.dto.AuthTokenResponse
import com.whatsappclone.core.network.model.dto.SendOtpRequest
import com.whatsappclone.core.network.model.dto.SendOtpResponse
import com.whatsappclone.core.network.model.dto.UserDto
import com.whatsappclone.core.network.model.dto.VerifyOtpRequest
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.network.model.safeApiCallUnit
import com.whatsappclone.core.network.token.TokenManager
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val notificationApi: NotificationApi
) : AuthRepository {

    override suspend fun sendOtp(phone: String): AppResult<SendOtpResponse> {
        return safeApiCall { authApi.sendOtp(SendOtpRequest(phone)) }
    }

    override suspend fun verifyOtp(phone: String, otp: String): AppResult<AuthTokenResponse> {
        val result = safeApiCall { authApi.verifyOtp(VerifyOtpRequest(phone, otp)) }

        if (result is AppResult.Success) {
            val data = result.data
            tokenManager.saveTokens(data.accessToken, data.refreshToken)
            userDao.upsert(data.user.toEntity())
        }

        return result
    }

    override suspend fun logout(): AppResult<Unit> {
        val result = safeApiCallUnit { authApi.logout() }
        tokenManager.clearTokens()
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
    }
}
