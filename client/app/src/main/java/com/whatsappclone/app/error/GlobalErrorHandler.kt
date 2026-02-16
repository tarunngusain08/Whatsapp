package com.whatsappclone.app.error

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.network.token.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class GlobalError {
    data object SessionExpired : GlobalError()
}

@Singleton
class GlobalErrorHandler @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val _errors = MutableSharedFlow<GlobalError>(extraBufferCapacity = 16)
    val errors: SharedFlow<GlobalError> = _errors.asSharedFlow()

    suspend fun handle(error: AppResult.Error) {
        when (error.code) {
            ErrorCode.UNAUTHORIZED -> {
                val refreshed = tokenManager.refreshToken()
                if (!refreshed) {
                    tokenManager.clearTokens()
                    _errors.emit(GlobalError.SessionExpired)
                }
            }
            else -> { /* Handled locally by ViewModel */ }
        }
    }
}
