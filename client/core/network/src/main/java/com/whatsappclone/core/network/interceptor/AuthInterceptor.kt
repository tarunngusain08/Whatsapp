package com.whatsappclone.core.network.interceptor

import com.whatsappclone.core.network.token.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val refreshMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth for token refresh and OTP endpoints to prevent infinite loops
        if (path.contains("auth/token/refresh") ||
            path.contains("auth/otp") ||
            path.contains("auth/refresh")
        ) {
            return chain.proceed(originalRequest)
        }

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
            val refreshSucceeded = runBlocking {
                refreshMutex.withLock {
                    // Check if another thread already refreshed the token
                    val currentToken = tokenManager.getAccessToken()
                    if (currentToken != null && currentToken != accessToken) {
                        true
                    } else {
                        tokenManager.refreshToken()
                    }
                }
            }

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
