package com.whatsappclone.core.network.model

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<ApiResponse<T>>
): AppResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.data != null) {
                AppResult.Success(body.data)
            } else if (body != null && body.error != null) {
                AppResult.Error(
                    code = mapErrorCode(response.code()),
                    message = body.error.message
                )
            } else {
                AppResult.Error(
                    code = ErrorCode.UNKNOWN,
                    message = body?.error?.message ?: "Unexpected response format"
                )
            }
        } else {
            val errorCode = mapHttpErrorCode(response.code())
            AppResult.Error(
                code = errorCode,
                message = response.message().ifEmpty { "HTTP ${response.code()}" }
            )
        }
    } catch (e: SocketTimeoutException) {
        AppResult.Error(
            code = ErrorCode.TIMEOUT,
            message = "Request timed out",
            cause = e
        )
    } catch (e: IOException) {
        AppResult.Error(
            code = ErrorCode.NETWORK_ERROR,
            message = e.message ?: "Network error occurred",
            cause = e
        )
    } catch (e: Exception) {
        AppResult.Error(
            code = ErrorCode.UNKNOWN,
            message = e.message ?: "An unexpected error occurred",
            cause = e
        )
    }
}

/**
 * Overload for API calls that return no data (Unit response).
 */
suspend fun safeApiCallUnit(
    apiCall: suspend () -> Response<ApiResponse<Unit>>
): AppResult<Unit> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                AppResult.Success(Unit)
            } else if (body != null && body.error != null) {
                AppResult.Error(
                    code = mapErrorCode(response.code()),
                    message = body.error.message
                )
            } else {
                AppResult.Success(Unit)
            }
        } else {
            val errorCode = mapHttpErrorCode(response.code())
            AppResult.Error(
                code = errorCode,
                message = response.message().ifEmpty { "HTTP ${response.code()}" }
            )
        }
    } catch (e: SocketTimeoutException) {
        AppResult.Error(
            code = ErrorCode.TIMEOUT,
            message = "Request timed out",
            cause = e
        )
    } catch (e: IOException) {
        AppResult.Error(
            code = ErrorCode.NETWORK_ERROR,
            message = e.message ?: "Network error occurred",
            cause = e
        )
    } catch (e: Exception) {
        AppResult.Error(
            code = ErrorCode.UNKNOWN,
            message = e.message ?: "An unexpected error occurred",
            cause = e
        )
    }
}

private fun mapHttpErrorCode(httpCode: Int): ErrorCode {
    return when (httpCode) {
        401 -> ErrorCode.UNAUTHORIZED
        403 -> ErrorCode.FORBIDDEN
        404 -> ErrorCode.NOT_FOUND
        409 -> ErrorCode.CONFLICT
        422 -> ErrorCode.VALIDATION_ERROR
        429 -> ErrorCode.RATE_LIMITED
        in 500..599 -> ErrorCode.SERVER_ERROR
        else -> ErrorCode.UNKNOWN
    }
}

private fun mapErrorCode(httpCode: Int): ErrorCode {
    return mapHttpErrorCode(httpCode)
}
