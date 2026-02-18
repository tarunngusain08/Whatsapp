package com.whatsappclone.feature.chat.data

import android.content.SharedPreferences
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.common.util.UuidGenerator
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.network.api.MessageApi
import com.whatsappclone.core.network.model.dto.MarkReadRequest
import com.whatsappclone.core.network.model.dto.MessageDto
import com.whatsappclone.core.network.model.dto.MessagePayloadDto
import com.whatsappclone.core.network.model.dto.SendMessageRequest
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.network.model.safeApiCallUnit
import com.whatsappclone.core.network.websocket.WebSocketManager
import com.whatsappclone.core.network.websocket.WsConnectionState
import com.whatsappclone.core.network.websocket.WsFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val webSocketManager: WebSocketManager,
    private val json: Json,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) : MessageRepository {

    companion object {
        private const val TAG = "MessageRepository"
        private const val PAGE_SIZE = 30
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    // ── Observe (paging) ─────────────────────────────────────────────────

    @OptIn(ExperimentalPagingApi::class)
    override fun observeMessages(chatId: String): Flow<PagingData<MessageEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE / 2,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE
            ),
            remoteMediator = MessageRemoteMediator(
                chatId = chatId,
                messageApi = messageApi,
                messageDao = messageDao
            ),
            pagingSourceFactory = { messageDao.pagingSource(chatId) }
        ).flow
    }

    // ── Save & Send ──────────────────────────────────────────────────────

    override suspend fun saveAndSend(
        chatId: String,
        content: String,
        messageType: String,
        replyToMessageId: String?
    ): AppResult<Unit> {
        val currentUserId = getCurrentUserId() ?: return AppResult.Error(
            code = ErrorCode.UNAUTHORIZED,
            message = "User not logged in"
        )

        val clientMsgId = UuidGenerator.generate()
        val now = System.currentTimeMillis()

        val messageEntity = MessageEntity(
            messageId = clientMsgId,
            clientMsgId = clientMsgId,
            chatId = chatId,
            senderId = currentUserId,
            messageType = messageType,
            content = content,
            replyToMessageId = replyToMessageId,
            status = "pending",
            timestamp = now,
            createdAt = now
        )

        messageDao.insert(messageEntity)

        chatDao.updateLastMessage(
            chatId = chatId,
            messageId = clientMsgId,
            preview = content.take(100),
            timestamp = now,
            updatedAt = now
        )

        if (webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
            val payload = buildJsonObject {
                put("chat_id", JsonPrimitive(chatId))
                put("client_msg_id", JsonPrimitive(clientMsgId))
                put("type", JsonPrimitive(messageType))
                put("payload", buildJsonObject {
                    put("body", JsonPrimitive(content))
                })
                if (replyToMessageId != null) {
                    put("reply_to_message_id", JsonPrimitive(replyToMessageId))
                }
            }
            val sent = webSocketManager.send(
                WsFrame(event = "message.send", data = payload, ref = clientMsgId)
            )
            if (sent) {
                messageDao.updateStatus(clientMsgId, "sending")
            } else {
                Log.w(TAG, "WebSocket send failed for $clientMsgId, will retry via REST")
            }
        } else {
            Log.d(TAG, "WS not connected, message $clientMsgId will be picked up by PendingMessageWorker")
        }

        return AppResult.Success(Unit)
    }

    // ── Remote insert ────────────────────────────────────────────────────

    override suspend fun insertFromRemote(messageDto: MessageDto) {
        val entity = messageDto.toEntity()

        val existing = messageDao.getByClientMsgId(entity.clientMsgId)
        if (existing != null) return

        messageDao.insert(entity)
    }

    // ── Confirm sent ─────────────────────────────────────────────────────

    override suspend fun confirmSent(clientMsgId: String, serverId: String, timestamp: Long) {
        messageDao.confirmSent(clientMsgId, serverId)
    }

    // ── Status update ────────────────────────────────────────────────────

    override suspend fun updateStatus(messageId: String, status: String) {
        messageDao.updateStatus(messageId, status)
    }

    // ── Delete ───────────────────────────────────────────────────────────

    override suspend fun softDelete(messageId: String, forEveryone: Boolean) {
        messageDao.softDelete(messageId, forEveryone)
    }

    // ── Star / Unstar ────────────────────────────────────────────────────

    override suspend fun starMessage(messageId: String, starred: Boolean): AppResult<Unit> {
        messageDao.setStarred(messageId, starred)

        val message = messageDao.getById(messageId) ?: return AppResult.Error(
            code = ErrorCode.NOT_FOUND,
            message = "Message not found"
        )

        val result = if (starred) {
            safeApiCallUnit { messageApi.starMessage(message.chatId, messageId) }
        } else {
            safeApiCallUnit { messageApi.unstarMessage(message.chatId, messageId) }
        }

        if (result is AppResult.Error) {
            messageDao.setStarred(messageId, !starred)
        }
        return result
    }

    // ── Mark read ────────────────────────────────────────────────────────

    override suspend fun markRead(chatId: String, upToMessageId: String): AppResult<Unit> {
        chatDao.updateUnreadCount(chatId, count = 0, updatedAt = System.currentTimeMillis())

        if (webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
            val payload = buildJsonObject {
                put("message_id", JsonPrimitive(upToMessageId))
            }
            webSocketManager.send(WsFrame(event = "message.read", data = payload))
        }

        return safeApiCallUnit {
            messageApi.markRead(chatId, MarkReadRequest(upToMessageId))
        }
    }

    // ── Pending messages ─────────────────────────────────────────────────

    override suspend fun getAllPending(): List<MessageEntity> =
        messageDao.getAllPendingMessages()

    // ── REST fallback send ───────────────────────────────────────────────

    override suspend fun sendViaRest(message: MessageEntity): AppResult<Unit> {
        val request = SendMessageRequest(
            clientMsgId = message.clientMsgId,
            type = message.messageType,
            payload = MessagePayloadDto(
                body = message.content,
                mediaId = message.mediaId,
                mediaUrl = message.mediaUrl,
                thumbnailUrl = message.mediaThumbnailUrl,
                mimeType = message.mediaMimeType,
                fileSize = message.mediaSize,
                duration = message.mediaDuration
            )
        )

        val result = safeApiCall { messageApi.sendMessage(message.chatId, request) }
        return when (result) {
            is AppResult.Success -> {
                val serverMsg = result.data
                messageDao.confirmSent(message.clientMsgId, serverMsg.messageId)
                messageDao.updateStatus(serverMsg.messageId, serverMsg.status)
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Error(
                code = ErrorCode.UNKNOWN,
                message = "Unexpected loading state"
            )
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun getCurrentUserId(): String? {
        return try {
            encryptedPrefs.getString(KEY_CURRENT_USER_ID, null)?.let { return it }
            extractUserIdFromJwt()?.also { userId ->
                try {
                    encryptedPrefs.edit()
                        .putString(KEY_CURRENT_USER_ID, userId)
                        .apply()
                } catch (_: Exception) { /* best-effort cache */ }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read current user id from prefs", e)
            extractUserIdFromJwt()
        }
    }

    private fun extractUserIdFromJwt(): String? {
        return try {
            val token = encryptedPrefs.getString("access_token", null)
                ?: return null
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(
                android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE),
                Charsets.UTF_8
            )
            org.json.JSONObject(payload).optString("user_id", null)
        } catch (_: Exception) {
            null
        }
    }
}

// ── Local mapper extensions (duplicated from :app module) ────────────────

private fun String.toEpochMillisOrNull(): Long? = try {
    java.time.Instant.parse(this).toEpochMilli()
} catch (_: Exception) {
    null
}

private fun MessageDto.toEntity(): MessageEntity = MessageEntity(
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
    replyToMessageId = null,
    status = status,
    isDeleted = isDeleted,
    isStarred = isStarred,
    timestamp = createdAt.toEpochMillisOrNull() ?: System.currentTimeMillis(),
    createdAt = System.currentTimeMillis()
)
