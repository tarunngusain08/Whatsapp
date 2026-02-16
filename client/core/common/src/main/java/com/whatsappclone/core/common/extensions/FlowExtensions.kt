package com.whatsappclone.core.common.extensions

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResult(): Flow<AppResult<T>> =
    this.map<T, AppResult<T>> { AppResult.Success(it) }
        .onStart { emit(AppResult.Loading) }
        .catch { emit(AppResult.Error(ErrorCode.UNKNOWN, it.message ?: "Unknown error", it)) }
