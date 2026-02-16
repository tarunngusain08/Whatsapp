package com.whatsappclone.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "chat_participants",
    primaryKeys = ["chatId", "userId"]
)
data class ChatParticipantEntity(
    val chatId: String,
    val userId: String,
    val role: String = "member",  // "admin" | "member"
    val joinedAt: Long
)
