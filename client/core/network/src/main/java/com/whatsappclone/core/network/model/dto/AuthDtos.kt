package com.whatsappclone.core.network.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    val phone: String
)

@Serializable
data class SendOtpResponse(
    val message: String,
    @SerialName("expires_in_seconds")
    val expiresInSeconds: Int,
    val otp: String? = null
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val otp: String
)

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("expires_in_seconds")
    val expiresInSeconds: Int,
    val user: UserDto,
    @SerialName("is_new_user")
    val isNewUser: Boolean = false
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)
