package com.whatsappclone.core.network.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    @SerialName("media_id")
    val mediaId: String,
    val url: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_type")
    val type: String,
    @SerialName("size_bytes")
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("duration_ms")
    val durationMs: Int? = null
)

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val platform: String = "android"
)
