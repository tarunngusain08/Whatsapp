package com.whatsappclone.app.data.websocket

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.room.withTransaction
import com.whatsappclone.core.database.AppDatabase
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import com.whatsappclone.core.network.api.ChatApi
import com.whatsappclone.core.network.api.MessageApi
import com.whatsappclone.core.network.model.dto.ChatDto
import com.whatsappclone.core.network.model.dto.MessagePayloadDto
import com.whatsappclone.core.network.model.dto.SendMessageRequest
import com.whatsappclone.core.network.websocket.WebSocketManager
import com.whatsappclone.core.network.websocket.WsConnectionState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncOnReconnectManager @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val chatApi: ChatApi,
    private val messageApi: MessageApi,
    private val chatDao: ChatDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val database: AppDatabase,
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private const val TAG = "SyncOnReconnect"
        private val KEY_LAST_SYNC = longPreferencesKey("ws_last_sync_timestamp")
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception", throwable)
    }
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val started = AtomicBoolean(false)
    private val syncMutex = Mutex()

    fun shutdown() {
        started.set(false)
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            webSocketManager.connectionState
                .filter { it == WsConnectionState.CONNECTED }
                .collect { performSync() }
        }
    }

    private suspend fun performSync() {
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "Sync already in progress, skipping")
            return
        }
        try {
            Log.d(TAG, "Connection established, starting sync...")
            try { userDao.setAllOffline() } catch (e: Exception) { Log.e(TAG, "Failed to reset online status", e) }
            try { syncChats() } catch (e: Exception) { Log.e(TAG, "Failed to sync chats", e) }
            try { flushPendingMessages() } catch (e: Exception) { Log.e(TAG, "Failed to flush pending", e) }
            try { updateLastSyncTimestamp() } catch (e: Exception) { Log.e(TAG, "Failed to update timestamp", e) }
            Log.d(TAG, "Sync completed")
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun syncChats() {
        var cursor: String? = null
        do {
            val response = chatApi.getChats(cursor = cursor, limit = 50)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.success || body.data == null) break
            val page = body.data!!
            database.withTransaction {
                val entities = page.items.map { dto ->
                    val existing = chatDao.getChatById(dto.chatId)
                    dto.toEntity(existingPinned = existing?.isPinned ?: false)
                }
                chatDao.upsertAll(entities)
                page.items.forEach { chatDto ->
                    chatDto.participants?.forEach { p ->
                        chatParticipantDao.upsert(ChatParticipantEntity(
                            chatId = chatDto.chatId, userId = p.userId, role = p.role,
                            joinedAt = System.currentTimeMillis()
                        ))
                    }
                }
            }
            cursor = page.nextCursor
        } while (page.hasMore && cursor != null)
    }

    private suspend fun flushPendingMessages() {
        val pending = messageDao.getAllPendingMessages()
        if (pending.isEmpty()) return
        for (message in pending) {
            try {
                val request = SendMessageRequest(
                    clientMsgId = message.clientMsgId, type = message.messageType,
                    payload = MessagePayloadDto(
                        body = message.content, mediaId = message.mediaId,
                        mediaUrl = message.mediaUrl, thumbnailUrl = message.mediaThumbnailUrl,
                        mimeType = message.mediaMimeType, fileSize = message.mediaSize,
                        durationMs = message.mediaDuration
                    )
                )
                val response = messageApi.sendMessage(chatId = message.chatId, request = request)
                if (response.isSuccessful) {
                    response.body()?.data?.let { serverMsg ->
                        messageDao.confirmSent(clientMsgId = message.clientMsgId, serverMessageId = serverMsg.messageId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing message ${message.clientMsgId}", e)
            }
        }
    }

    private suspend fun updateLastSyncTimestamp() {
        dataStore.edit { it[KEY_LAST_SYNC] = System.currentTimeMillis() }
    }

    private fun ChatDto.toEntity(existingPinned: Boolean = false): ChatEntity = ChatEntity(
        chatId = chatId, chatType = type, name = name, description = description,
        avatarUrl = avatarUrl, lastMessageId = lastMessage?.messageId,
        lastMessagePreview = lastMessage?.preview,
        lastMessageTimestamp = lastMessage?.timestamp?.let { parseTimestamp(it) },
        unreadCount = unreadCount, isMuted = isMuted, isPinned = existingPinned,
        createdAt = createdAt?.let { parseTimestamp(it) } ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.let { parseTimestamp(it) } ?: System.currentTimeMillis()
    )

    private fun parseTimestamp(isoString: String): Long = try {
        java.time.Instant.parse(isoString).toEpochMilli()
    } catch (_: Exception) { System.currentTimeMillis() }
}
