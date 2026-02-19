package com.whatsappclone.core.network.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("client_msg_id")
    val clientMsgId: String? = null,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("sender_id")
    val senderId: String,
    val type: String,
    val payload: MessagePayloadDto,
    val status: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("is_starred")
    val isStarred: Boolean = false,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class MessagePayloadDto(
    val body: String? = null,
    @SerialName("media_id")
    val mediaId: String? = null,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
    @SerialName("file_name")
    val fileName: String? = null,
    @SerialName("file_size")
    val fileSize: Long? = null,
    val duration: Int? = null
)

@Serializable
data class SendMessageRequest(
    @SerialName("client_msg_id")
    val clientMsgId: String,
    val type: String,
    val payload: MessagePayloadDto
)

@Serializable
data class MarkReadRequest(
    @SerialName("up_to_message_id")
    val upToMessageId: String
)

@Serializable
data class ReceiptDto(
    @SerialName("user_id")
    val userId: String,
    val status: String,
    @SerialName("updated_at")
    val updatedAt: String
)
