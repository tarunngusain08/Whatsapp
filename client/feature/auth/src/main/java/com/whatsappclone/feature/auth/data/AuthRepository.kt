package com.whatsappclone.feature.auth.data

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.network.model.dto.AuthTokenResponse
import com.whatsappclone.core.network.model.dto.SendOtpResponse

interface AuthRepository {

    suspend fun sendOtp(phone: String): AppResult<SendOtpResponse>

    suspend fun verifyOtp(phone: String, otp: String): AppResult<AuthTokenResponse>

    suspend fun logout(): AppResult<Unit>

    fun isLoggedIn(): Boolean
}
