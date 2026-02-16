package com.whatsappclone.feature.chat.model

data class Reaction(
    val emoji: String,
    val userIds: List<String>,
    val isFromMe: Boolean
)

data class MessageUi(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val senderName: String?,
    val content: String?,
    val messageType: String,
    val status: String,
    val isOwnMessage: Boolean,
    val isDeleted: Boolean,
    val isStarred: Boolean,
    val mediaUrl: String?,
    val mediaThumbnailUrl: String?,
    val replyToMessageId: String?,
    val formattedTime: String,
    val timestamp: Long,
    val showDateSeparator: Boolean = false,
    val dateSeparatorText: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val replyToType: String? = null,
    val replyToMediaThumbnailUrl: String? = null,
    val isForwarded: Boolean = false,
    val reactions: List<Reaction> = emptyList()
)
