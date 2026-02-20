package com.whatsappclone.app.data.websocket

import android.content.SharedPreferences
import android.util.Log
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.feature.chat.call.CallService
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.network.model.dto.ChatDto
import com.whatsappclone.core.network.model.dto.MessageDto
import com.whatsappclone.core.network.websocket.ServerWsEvent
import com.whatsappclone.core.network.websocket.TypingStateHolder
import com.whatsappclone.core.network.websocket.WebSocketManager
import com.whatsappclone.core.network.websocket.WsConnectionState
import com.whatsappclone.core.network.websocket.WsFrame
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WsEventRouter @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val typingStateHolder: TypingStateHolder,
    private val callService: CallService,
    private val json: Json,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) {

    companion object {
        private const val TAG = "WsEventRouter"
    }

    private data class PendingReceipt(
        val messageId: String,
        val senderId: String,
        val chatId: String
    )

    private val pendingReceipts = mutableListOf<PendingReceipt>()

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

        scope.launch {
            webSocketManager.connectionState.collect { state ->
                if (state == WsConnectionState.CONNECTED) {
                    flushPendingReceipts()
                }
            }
        }
    }

    private fun flushPendingReceipts() {
        val toSend: List<PendingReceipt>
        synchronized(pendingReceipts) {
            toSend = pendingReceipts.toList()
            pendingReceipts.clear()
        }
        for (receipt in toSend) {
            sendDeliveryReceipt(receipt.messageId, receipt.senderId, receipt.chatId)
        }
    }

    private suspend fun routeEvent(event: ServerWsEvent) {
        when (event) {
            is ServerWsEvent.NewMessage -> handleNewMessage(event)
            is ServerWsEvent.MessageSent -> handleMessageSent(event)
            is ServerWsEvent.MessageStatus -> handleMessageStatus(event)
            is ServerWsEvent.MessageDeleted -> handleMessageDeleted(event)
            is ServerWsEvent.MessageReaction -> handleMessageReaction(event)
            is ServerWsEvent.TypingEvent -> handleTyping(event)
            is ServerWsEvent.PresenceEvent -> handlePresence(event)
            is ServerWsEvent.ChatCreated -> handleChatCreated(event)
            is ServerWsEvent.ChatUpdated -> handleChatUpdated(event)
            is ServerWsEvent.GroupMemberAdded -> handleMemberAdded(event)
            is ServerWsEvent.GroupMemberRemoved -> handleMemberRemoved(event)
            is ServerWsEvent.CallOffer -> handleCallOffer(event)
            is ServerWsEvent.CallAnswer -> handleCallAnswer(event)
            is ServerWsEvent.CallIceCandidate -> handleCallIceCandidate(event)
            is ServerWsEvent.CallEnd -> handleCallEnd(event)
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

        sendDeliveryReceipt(messageDto.messageId, messageDto.senderId, event.chatId)
    }

    private fun sendDeliveryReceipt(messageId: String, senderId: String, chatId: String) {
        try {
            val currentUserId = encryptedPrefs.getString("current_user_id", null)
            if (currentUserId == null || senderId == currentUserId) return
            if (webSocketManager.connectionState.value != WsConnectionState.CONNECTED) {
                synchronized(pendingReceipts) {
                    pendingReceipts.add(PendingReceipt(messageId, senderId, chatId))
                }
                return
            }

            val payload = buildJsonObject {
                put("message_id", JsonPrimitive(messageId))
                put("chat_id", JsonPrimitive(chatId))
                put("sender_id", JsonPrimitive(senderId))
            }
            webSocketManager.send(WsFrame(event = "message.delivered", data = payload))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send delivery receipt for $messageId", e)
        }
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
        val existing = messageDao.getById(event.messageId) ?: return
        val newRank = statusRank(event.status)
        if (newRank < 0) {
            Log.w(TAG, "Ignoring unknown status '${event.status}' for message ${event.messageId}")
            return
        }
        if (newRank > statusRank(existing.status)) {
            messageDao.updateStatus(messageId = event.messageId, status = event.status)
        }
    }

    private fun statusRank(status: String): Int = when (status) {
        "pending" -> 0
        "sending" -> 1
        "sent" -> 2
        "delivered" -> 3
        "read" -> 4
        else -> -1
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

    private suspend fun handleMessageReaction(event: ServerWsEvent.MessageReaction) {
        val existingJson = messageDao.getReactionsJson(event.messageId)
        val reactions = parseReactionsJson(existingJson)

        val updated = if (event.removed) {
            reactions.mapNotNull { (emoji, userIds) ->
                val filtered = userIds - event.userId
                if (filtered.isEmpty()) null else emoji to filtered
            }.toMap()
        } else {
            val existing = reactions.toMutableMap()
            val users = existing.getOrDefault(event.emoji, emptyList()).toMutableList()
            if (event.userId !in users) users.add(event.userId)
            existing[event.emoji] = users
            existing
        }

        messageDao.updateReactions(event.messageId, serializeReactionsJson(updated))
    }

    private fun parseReactionsJson(jsonStr: String?): Map<String, List<String>> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        return try {
            val obj = json.decodeFromString(JsonObject.serializer(), jsonStr)
            obj.mapValues { (_, value) ->
                json.decodeFromString<List<String>>(value.toString())
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeReactionsJson(reactions: Map<String, List<String>>): String {
        return buildJsonObject {
            reactions.forEach { (emoji, userIds) ->
                put(emoji, kotlinx.serialization.json.JsonArray(userIds.map { JsonPrimitive(it) }))
            }
        }.toString()
    }

    private fun handleCallOffer(event: ServerWsEvent.CallOffer) {
        Log.i(TAG, "Incoming call offer: callId=${event.callId} from=${event.callerId} type=${event.callType}")
        callService.onIncomingOffer(
            callId = event.callId,
            callerId = event.callerId,
            callerName = event.callerId.take(8),
            callerAvatar = null,
            sdp = event.sdp,
            callType = event.callType
        )
    }

    private fun handleCallAnswer(event: ServerWsEvent.CallAnswer) {
        Log.i(TAG, "Call answered: callId=${event.callId} by=${event.answererId}")
        callService.onRemoteAnswer(event.sdp)
    }

    private fun handleCallIceCandidate(event: ServerWsEvent.CallIceCandidate) {
        Log.d(TAG, "ICE candidate: callId=${event.callId} from=${event.senderId}")
        callService.onRemoteIceCandidate(event.candidate)
    }

    private fun handleCallEnd(event: ServerWsEvent.CallEnd) {
        Log.i(TAG, "Call ended: callId=${event.callId} reason=${event.reason}")
        callService.onRemoteEnd(event.reason)
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
        replyToMessageId = replyToMessageId, status = status, isDeleted = isDeleted,
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
