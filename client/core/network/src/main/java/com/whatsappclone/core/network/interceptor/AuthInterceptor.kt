package com.whatsappclone.core.network.interceptor

import com.whatsappclone.core.network.token.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val accessToken = tokenManager.getAccessToken()
        val authenticatedRequest = if (accessToken != null) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(authenticatedRequest)

        if (response.code == HTTP_UNAUTHORIZED && accessToken != null) {
            val refreshSucceeded = runBlocking { tokenManager.refreshToken() }

            if (refreshSucceeded) {
                response.close()

                val newAccessToken = tokenManager.getAccessToken()
                val retryRequest = originalRequest.newBuilder()
                    .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX $newAccessToken")
                    .build()

                return chain.proceed(retryRequest)
            }
        }

        return response
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer"
        private const val HTTP_UNAUTHORIZED = 401
    }
}
