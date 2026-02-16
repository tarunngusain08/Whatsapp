package com.whatsappclone.core.network.token

interface TokenManager {

    fun getAccessToken(): String?

    fun getRefreshToken(): String?

    fun saveTokens(accessToken: String, refreshToken: String)

    suspend fun refreshToken(): Boolean

    suspend fun clearTokens()

    fun isLoggedIn(): Boolean
}
