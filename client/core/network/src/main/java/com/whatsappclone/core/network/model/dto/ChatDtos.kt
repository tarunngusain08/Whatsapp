package com.whatsappclone.core.network.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatDto(
    @SerialName("chat_id")
    val chatId: String,
    val type: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val participants: List<ChatParticipantDto>? = null,
    @SerialName("last_message")
    val lastMessage: LastMessageDto? = null,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    @SerialName("is_muted")
    val isMuted: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class ChatParticipantDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val role: String
)

@Serializable
data class LastMessageDto(
    @SerialName("message_id")
    val messageId: String,
    val preview: String? = null,
    @SerialName("sender_id")
    val senderId: String? = null,
    val type: String? = null,
    val timestamp: String? = null
)

@Serializable
data class CreateChatRequest(
    val type: String,
    @SerialName("participant_ids")
    val participantIds: List<String>,
    val name: String? = null
)

@Serializable
data class MuteChatRequest(
    val muted: Boolean
)

@Serializable
data class AddParticipantsRequest(
    @SerialName("user_ids")
    val userIds: List<String>
)

@Serializable
data class UpdateRoleRequest(
    val role: String
)

@Serializable
data class DisappearingTimerRequest(
    val timer: String
)
