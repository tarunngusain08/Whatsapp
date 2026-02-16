package com.whatsappclone.core.network.api

import com.whatsappclone.core.network.model.ApiResponse
import com.whatsappclone.core.network.model.dto.AuthTokenResponse
import com.whatsappclone.core.network.model.dto.RefreshRequest
import com.whatsappclone.core.network.model.dto.SendOtpRequest
import com.whatsappclone.core.network.model.dto.SendOtpResponse
import com.whatsappclone.core.network.model.dto.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/otp/send")
    suspend fun sendOtp(
        @Body request: SendOtpRequest
    ): Response<ApiResponse<SendOtpResponse>>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<ApiResponse<AuthTokenResponse>>

    @POST("auth/token/refresh")
    suspend fun refreshToken(
        @Body request: RefreshRequest
    ): Response<ApiResponse<AuthTokenResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
