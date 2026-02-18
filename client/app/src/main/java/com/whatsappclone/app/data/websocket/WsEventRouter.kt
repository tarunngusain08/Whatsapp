package com.whatsappclone.app.data.websocket

import android.util.Log
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.network.model.dto.ChatDto
import com.whatsappclone.core.network.model.dto.MessageDto
import com.whatsappclone.core.network.websocket.ServerWsEvent
import com.whatsappclone.core.network.websocket.TypingStateHolder
import com.whatsappclone.core.network.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WsEventRouter @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val typingStateHolder: TypingStateHolder,
    private val json: Json
) {

    companion object {
        private const val TAG = "WsEventRouter"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            webSocketManager.events.collect { event ->
                try {
                    routeEvent(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error routing event: $event", e)
                }
            }
        }
    }

    private suspend fun routeEvent(event: ServerWsEvent) {
        when (event) {
            is ServerWsEvent.NewMessage -> handleNewMessage(event)
            is ServerWsEvent.MessageSent -> handleMessageSent(event)
            is ServerWsEvent.MessageStatus -> handleMessageStatus(event)
            is ServerWsEvent.MessageDeleted -> handleMessageDeleted(event)
            is ServerWsEvent.TypingEvent -> handleTyping(event)
            is ServerWsEvent.PresenceEvent -> handlePresence(event)
            is ServerWsEvent.ChatCreated -> handleChatCreated(event)
            is ServerWsEvent.ChatUpdated -> handleChatUpdated(event)
            is ServerWsEvent.GroupMemberAdded -> handleMemberAdded(event)
            is ServerWsEvent.GroupMemberRemoved -> handleMemberRemoved(event)
            is ServerWsEvent.Error -> handleError(event)
        }
    }

    private suspend fun handleNewMessage(event: ServerWsEvent.NewMessage) {
        val messageDto = json.decodeFromString(MessageDto.serializer(), event.messageJson)
        val entity = messageDto.toEntity()
        messageDao.insert(entity)
        val preview = buildMessagePreview(messageDto)
        val timestampMs = parseTimestamp(messageDto.createdAt)
        chatDao.updateLastMessage(
            chatId = event.chatId,
            messageId = messageDto.messageId,
            preview = preview,
            timestamp = timestampMs,
            updatedAt = System.currentTimeMillis()
        )
        chatDao.incrementUnreadCount(chatId = event.chatId, updatedAt = System.currentTimeMillis())
    }

    private suspend fun handleMessageSent(event: ServerWsEvent.MessageSent) {
        messageDao.confirmSent(
            clientMsgId = event.clientMsgId,
            serverMessageId = event.messageId
        )
        val existing = messageDao.getByClientMsgId(event.clientMsgId)
        chatDao.updateLastMessage(
            chatId = event.chatId,
            messageId = event.messageId,
            preview = existing?.content,
            timestamp = parseTimestamp(event.timestamp),
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun handleMessageStatus(event: ServerWsEvent.MessageStatus) {
        messageDao.updateStatus(messageId = event.messageId, status = event.status)
    }

    private suspend fun handleMessageDeleted(event: ServerWsEvent.MessageDeleted) {
        messageDao.softDelete(messageId = event.messageId, forEveryone = event.deletedForEveryone)
    }

    private fun handleTyping(event: ServerWsEvent.TypingEvent) {
        typingStateHolder.onTyping(chatId = event.chatId, userId = event.userId, isTyping = event.isTyping)
    }

    private suspend fun handlePresence(event: ServerWsEvent.PresenceEvent) {
        userDao.updatePresence(
            userId = event.userId,
            isOnline = event.online,
            lastSeen = event.lastSeen?.let { parseTimestamp(it) },
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun handleChatCreated(event: ServerWsEvent.ChatCreated) {
        val chatDto = json.decodeFromString(ChatDto.serializer(), event.chatJson)
        chatDao.upsert(chatDto.toEntity())
        chatDto.participants?.forEach { participant ->
            chatParticipantDao.upsert(
                ChatParticipantEntity(
                    chatId = chatDto.chatId,
                    userId = participant.userId,
                    role = participant.role,
                    joinedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun handleChatUpdated(event: ServerWsEvent.ChatUpdated) {
        val updateJson = json.decodeFromString(JsonObject.serializer(), event.updateJson)
        val existing = chatDao.getChatById(event.chatId) ?: return
        chatDao.upsert(existing.copy(
            name = updateJson["name"]?.jsonPrimitive?.content ?: existing.name,
            description = updateJson["description"]?.jsonPrimitive?.content ?: existing.description,
            avatarUrl = updateJson["avatar_url"]?.jsonPrimitive?.content ?: existing.avatarUrl,
            updatedAt = System.currentTimeMillis()
        ))
    }

    private suspend fun handleMemberAdded(event: ServerWsEvent.GroupMemberAdded) {
        chatParticipantDao.upsert(ChatParticipantEntity(
            chatId = event.chatId, userId = event.userId, role = "member",
            joinedAt = System.currentTimeMillis()
        ))
    }

    private suspend fun handleMemberRemoved(event: ServerWsEvent.GroupMemberRemoved) {
        chatParticipantDao.delete(chatId = event.chatId, userId = event.userId)
    }

    private fun handleError(event: ServerWsEvent.Error) {
        Log.e(TAG, "Server error: code=${event.code} message=${event.message}")
    }

    private fun MessageDto.toEntity(): MessageEntity = MessageEntity(
        messageId = messageId, clientMsgId = clientMsgId ?: messageId,
        chatId = chatId, senderId = senderId, messageType = type,
        content = payload.body, mediaId = payload.mediaId, mediaUrl = payload.mediaUrl,
        mediaThumbnailUrl = payload.thumbnailUrl, mediaMimeType = payload.mimeType,
        mediaSize = payload.fileSize, mediaDuration = payload.duration,
        replyToMessageId = null, status = status, isDeleted = isDeleted,
        deletedForEveryone = false, isStarred = isStarred,
        timestamp = parseTimestamp(createdAt), createdAt = parseTimestamp(createdAt)
    )

    private fun ChatDto.toEntity(): ChatEntity = ChatEntity(
        chatId = chatId, chatType = type, name = name, description = description,
        avatarUrl = avatarUrl, lastMessageId = lastMessage?.messageId,
        lastMessagePreview = lastMessage?.preview,
        lastMessageTimestamp = lastMessage?.timestamp?.let { parseTimestamp(it) },
        unreadCount = unreadCount, isMuted = isMuted, isPinned = false,
        createdAt = createdAt?.let { parseTimestamp(it) } ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.let { parseTimestamp(it) } ?: System.currentTimeMillis()
    )

    private fun buildMessagePreview(dto: MessageDto): String? = when (dto.type) {
        "text" -> dto.payload.body?.take(100)
        "image" -> "\uD83D\uDCF7 Photo"
        "video" -> "\uD83C\uDFA5 Video"
        "audio" -> "\uD83C\uDFA4 Audio"
        "document" -> "\uD83D\uDCC4 ${dto.payload.fileName ?: "Document"}"
        else -> dto.payload.body?.take(100)
    }

    private fun parseTimestamp(isoString: String): Long = try {
        Instant.parse(isoString).toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}
