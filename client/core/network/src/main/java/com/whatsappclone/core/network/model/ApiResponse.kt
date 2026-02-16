package com.whatsappclone.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ApiMeta? = null
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: List<String> = emptyList()
)

@Serializable
data class ApiMeta(
    val requestId: String? = null,
    val timestamp: String? = null
)

@Serializable
data class PaginatedData<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)
