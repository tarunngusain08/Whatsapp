package com.whatsappclone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    indices = [Index(value = ["lastMessageTimestamp"])]
)
data class ChatEntity(
    @PrimaryKey
    val chatId: String,
    val chatType: String,  // "direct" | "group"
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null,
    val lastMessageId: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
