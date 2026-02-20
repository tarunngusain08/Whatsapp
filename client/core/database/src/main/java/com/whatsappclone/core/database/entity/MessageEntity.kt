package com.whatsappclone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId", "timestamp"]),
        Index(value = ["chatId", "status"]),
        Index(value = ["clientMsgId"], unique = true),
        Index(value = ["senderId"]),
        Index(value = ["replyToMessageId"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val clientMsgId: String,
    val chatId: String,
    val senderId: String,
    val messageType: String,  // "text"|"image"|"video"|"audio"|"document"|"system"
    val content: String? = null,
    val mediaId: String? = null,
    val mediaUrl: String? = null,
    val mediaThumbnailUrl: String? = null,
    val mediaMimeType: String? = null,
    val mediaSize: Long? = null,
    val mediaDuration: Int? = null,
    val replyToMessageId: String? = null,
    val status: String = "pending",  // "pending"|"sent"|"delivered"|"read"|"scheduled"
    val isDeleted: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val isStarred: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val reactionsJson: String? = null,
    val timestamp: Long,
    val createdAt: Long,
    val scheduledAt: Long? = null
)
