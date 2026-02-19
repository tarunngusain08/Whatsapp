package com.whatsappclone.core.network.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    val type: String,
    val content: String,
    val caption: String? = null,
    @SerialName("bg_color")
    val bgColor: String? = null,
    val viewers: List<String> = emptyList(),
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("expires_at")
    val expiresAt: String
)

@Serializable
data class CreateStatusRequest(
    val type: String,
    val content: String,
    val caption: String? = null,
    @SerialName("bg_color")
    val bgColor: String? = null
)
