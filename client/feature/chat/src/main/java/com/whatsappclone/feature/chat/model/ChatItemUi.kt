package com.whatsappclone.feature.chat.model

data class ChatItemUi(
    val chatId: String,
    val name: String,
    val avatarUrl: String?,
    val lastMessagePreview: String?,
    val lastMessageTimestamp: Long?,
    val lastMessageSenderName: String?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val chatType: String,
    val typingUsers: Set<String> = emptySet(),
    val formattedTime: String = ""
)
