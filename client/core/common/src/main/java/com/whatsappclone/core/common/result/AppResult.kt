package com.whatsappclone.core.common.result

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(
        val code: ErrorCode,
        val message: String,
        val cause: Throwable? = null
    ) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()
}

fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
    is AppResult.Loading -> this
}

fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Success -> data
    else -> null
}

fun <T> AppResult<T>.getOrThrow(): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Error -> throw RuntimeException(message, cause)
    is AppResult.Loading -> throw IllegalStateException("Result is still loading")
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppResult.Error) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(this)
    return this
}
