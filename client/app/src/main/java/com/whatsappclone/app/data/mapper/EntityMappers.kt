package com.whatsappclone.app.data.mapper

import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import com.whatsappclone.core.database.entity.MediaEntity
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.core.network.model.dto.ChatDto
import com.whatsappclone.core.network.model.dto.ChatParticipantDto
import com.whatsappclone.core.network.model.dto.MediaUploadResponse
import com.whatsappclone.core.network.model.dto.MessageDto
import com.whatsappclone.core.network.model.dto.UserDto

fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    phone = phone,
    displayName = displayName,
    statusText = statusText,
    avatarUrl = avatarUrl,
    isOnline = isOnline ?: false,
    lastSeen = lastSeen?.toEpochMillisOrNull(),
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

fun ChatDto.toEntity(): ChatEntity = ChatEntity(
    chatId = chatId,
    chatType = type,
    name = name,
    description = description,
    avatarUrl = avatarUrl,
    lastMessageId = lastMessage?.messageId,
    lastMessagePreview = lastMessage?.preview,
    lastMessageTimestamp = lastMessage?.timestamp?.toEpochMillisOrNull(),
    unreadCount = unreadCount,
    isMuted = isMuted,
    createdAt = createdAt?.toEpochMillisOrNull() ?: System.currentTimeMillis(),
    updatedAt = updatedAt?.toEpochMillisOrNull() ?: System.currentTimeMillis()
)

fun ChatParticipantDto.toEntity(chatId: String): ChatParticipantEntity =
    ChatParticipantEntity(
        chatId = chatId,
        userId = userId,
        role = role,
        joinedAt = System.currentTimeMillis()
    )

fun ChatParticipantDto.toUserEntity(): UserEntity = UserEntity(
    id = userId,
    phone = "",
    displayName = displayName ?: "Unknown",
    avatarUrl = avatarUrl,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

fun MessageDto.toEntity(): MessageEntity = MessageEntity(
    messageId = messageId,
    clientMsgId = clientMsgId ?: messageId,
    chatId = chatId,
    senderId = senderId,
    messageType = type,
    content = payload.body,
    mediaId = payload.mediaId,
    mediaUrl = payload.mediaUrl,
    mediaThumbnailUrl = payload.thumbnailUrl,
    mediaMimeType = payload.mimeType,
    mediaSize = payload.fileSize,
    mediaDuration = payload.duration,
    replyToMessageId = replyToMessageId,
    status = status,
    isDeleted = isDeleted,
    isStarred = isStarred,
    timestamp = createdAt.toEpochMillisOrNull() ?: System.currentTimeMillis(),
    createdAt = System.currentTimeMillis()
)

fun MediaUploadResponse.toEntity(uploaderId: String): MediaEntity = MediaEntity(
    mediaId = mediaId,
    uploaderId = uploaderId,
    fileType = type,
    mimeType = mimeType,
    originalFilename = null,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    durationMs = durationMs,
    storageUrl = url,
    thumbnailUrl = thumbnailUrl,
    createdAt = System.currentTimeMillis()
)

fun String.toEpochMillisOrNull(): Long? = try {
    java.time.Instant.parse(this).toEpochMilli()
} catch (_: Exception) {
    null
}

fun String.toEpochMillis(): Long =
    toEpochMillisOrNull() ?: System.currentTimeMillis()
