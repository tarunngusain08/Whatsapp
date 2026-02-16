package com.whatsappclone.core.database.relation

import androidx.room.Embedded
import com.whatsappclone.core.database.entity.ChatEntity

data class ChatWithLastMessage(
    @Embedded
    val chat: ChatEntity,
    val lastMessageText: String?,
    val lastMessageType: String?,
    val lastMessageSenderId: String?,
    val lastMessageSenderName: String?
)
