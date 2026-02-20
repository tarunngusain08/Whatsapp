package com.whatsappclone.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "chat_participants",
    primaryKeys = ["chatId", "userId"],
    indices = [
        Index(value = ["userId"])
    ]
)
data class ChatParticipantEntity(
    val chatId: String,
    val userId: String,
    val role: String = "member",  // "admin" | "member"
    val joinedAt: Long
)
