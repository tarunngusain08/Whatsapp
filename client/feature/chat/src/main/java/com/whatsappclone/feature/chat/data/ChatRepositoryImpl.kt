package com.whatsappclone.feature.chat.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.map
import com.whatsappclone.core.database.AppDatabase
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.core.database.relation.ChatWithLastMessage
import com.whatsappclone.core.network.api.ChatApi
import com.whatsappclone.core.network.model.dto.ChatDto
import com.whatsappclone.core.network.model.dto.ChatParticipantDto
import com.whatsappclone.core.network.model.dto.CreateChatRequest
import com.whatsappclone.core.network.model.dto.MuteChatRequest
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.network.model.safeApiCallUnit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatDao: ChatDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val database: AppDatabase,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) : ChatRepository {

    @Volatile
    private var cachedCurrentUserId: String? = null

    // ── Observe ──────────────────────────────────────────────────────────

    override fun observeChats(currentUserId: String?): Flow<List<ChatWithLastMessage>> {
        val userId = currentUserId ?: getCurrentUserId() ?: ""
        return chatDao.observeChatsWithLastMessage(userId)
    }

    // ── Sync ─────────────────────────────────────────────────────────────

    override suspend fun syncChats(): AppResult<Unit> {
        var cursor: String? = null

        do {
            val result = safeApiCall { chatApi.getChats(cursor = cursor, limit = 50) }

            when (result) {
                is AppResult.Success -> {
                    val page = result.data
                    for (chatDto in page.items) {
                        upsertChatWithRelations(chatDto)
                    }
                    cursor = page.nextCursor
                    if (!page.hasMore) break
                }
                is AppResult.Error -> return result
                is AppResult.Loading -> break
            }
        } while (cursor != null)

        return AppResult.Success(Unit)
    }

    // ── Create ───────────────────────────────────────────────────────────

    override suspend fun createDirectChat(otherUserId: String): AppResult<String> {
        val currentUserId = getCurrentUserId()

        if (currentUserId != null) {
            val existingChatId = chatDao.findDirectChatWithUser(currentUserId, otherUserId)
            if (existingChatId != null) {
                return AppResult.Success(existingChatId)
            }
        }

        val request = CreateChatRequest(
            type = "direct",
            participantIds = listOf(otherUserId)
        )

        val result = safeApiCall { chatApi.createChat(request) }
        return when (result) {
            is AppResult.Success -> {
                val chatDto = result.data
                upsertChatWithRelations(chatDto)

                // Ensure participants are persisted locally even if the
                // backend response omitted them, so findDirectChatWithUser
                // works on subsequent lookups.
                if (chatDto.participants.isNullOrEmpty() && currentUserId != null) {
                    val now = System.currentTimeMillis()
                    chatParticipantDao.upsertAll(listOf(
                        ChatParticipantEntity(
                            chatId = chatDto.chatId,
                            userId = currentUserId,
                            role = "member",
                            joinedAt = now
                        ),
                        ChatParticipantEntity(
                            chatId = chatDto.chatId,
                            userId = otherUserId,
                            role = "member",
                            joinedAt = now
                        )
                    ))
                }

                AppResult.Success(chatDto.chatId)
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Error(
                code = com.whatsappclone.core.common.result.ErrorCode.UNKNOWN,
                message = "Unexpected loading state"
            )
        }
    }

    // ── Create Group ──────────────────────────────────────────────────────

    override suspend fun createGroupChat(
        name: String,
        participantIds: List<String>
    ): AppResult<String> {
        val request = CreateChatRequest(
            type = "group",
            participantIds = participantIds,
            name = name
        )

        val result = safeApiCall { chatApi.createChat(request) }
        return when (result) {
            is AppResult.Success -> {
                val chatDto = result.data
                upsertChatWithRelations(chatDto)
                AppResult.Success(chatDto.chatId)
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Error(
                code = com.whatsappclone.core.common.result.ErrorCode.UNKNOWN,
                message = "Unexpected loading state"
            )
        }
    }

    // ── Detail ───────────────────────────────────────────────────────────

    override suspend fun getChatDetail(chatId: String): AppResult<ChatEntity> {
        val cached = chatDao.getChatById(chatId)
        if (cached != null) return AppResult.Success(cached)

        val result = safeApiCall { chatApi.getChatDetail(chatId) }
        return when (result) {
            is AppResult.Success -> {
                val chatDto = result.data
                upsertChatWithRelations(chatDto)
                AppResult.Success(chatDto.toEntity())
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Error(
                code = com.whatsappclone.core.common.result.ErrorCode.UNKNOWN,
                message = "Unexpected loading state"
            )
        }
    }

    // ── Local mutations ──────────────────────────────────────────────────

    override suspend fun updateLastMessage(
        chatId: String,
        messageId: String,
        preview: String?,
        timestamp: Long
    ) {
        chatDao.updateLastMessage(
            chatId = chatId,
            messageId = messageId,
            preview = preview,
            timestamp = timestamp,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun incrementUnreadCount(chatId: String) {
        chatDao.incrementUnreadCount(chatId, updatedAt = System.currentTimeMillis())
    }

    override suspend fun resetUnreadCount(chatId: String) {
        chatDao.updateUnreadCount(chatId, count = 0, updatedAt = System.currentTimeMillis())
    }

    // ── Mute ─────────────────────────────────────────────────────────────

    override suspend fun muteChat(chatId: String, muted: Boolean): AppResult<Unit> {
        chatDao.setMuted(chatId, muted, updatedAt = System.currentTimeMillis())

        val result = safeApiCallUnit { chatApi.muteChat(chatId, MuteChatRequest(muted)) }
        if (result is AppResult.Error) {
            chatDao.setMuted(chatId, !muted, updatedAt = System.currentTimeMillis())
        }
        return result
    }

    // ── Pin ──────────────────────────────────────────────────────────────

    override suspend fun pinChat(chatId: String, pinned: Boolean) {
        chatDao.setPinned(chatId, pinned, updatedAt = System.currentTimeMillis())
    }

    // ── Archive ──────────────────────────────────────────────────────────

    override suspend fun archiveChat(chatId: String, archived: Boolean) {
        chatDao.setArchived(chatId, archived, updatedAt = System.currentTimeMillis())
    }

    // ── Delete ───────────────────────────────────────────────────────────

    override suspend fun deleteChat(chatId: String) {
        database.withTransaction {
            messageDao.deleteAllForChat(chatId)
            chatParticipantDao.deleteAllForChat(chatId)
            chatDao.deleteById(chatId)
        }
    }

    override fun observeArchivedChats(currentUserId: String?): Flow<List<ChatWithLastMessage>> {
        val userId = currentUserId ?: getCurrentUserId() ?: ""
        return chatDao.observeArchivedChats(userId)
    }

    override fun observeArchivedCount(): Flow<Int> {
        return chatDao.observeArchivedCount()
    }

    // ── Disappearing messages ──────────────────────────────────────────

    override suspend fun setDisappearingTimer(chatId: String, timer: String): AppResult<Unit> {
        return safeApiCallUnit {
            chatApi.setDisappearingTimer(chatId, com.whatsappclone.core.network.model.dto.DisappearingTimerRequest(timer))
        }
    }

    // ── Remote insert ────────────────────────────────────────────────────

    override suspend fun insertFromRemote(chatDto: ChatDto) {
        upsertChatWithRelations(chatDto)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private suspend fun upsertChatWithRelations(chatDto: ChatDto) {
        database.withTransaction {
            chatDao.upsert(chatDto.toEntity())

            chatDto.participants?.let { participants ->
                val participantEntities = participants.map { it.toEntity(chatDto.chatId) }
                chatParticipantDao.upsertAll(participantEntities)

                val userIds = participants.map { it.userId }
                val existingUsers = userDao.getByIds(userIds)
                val existingUserMap = existingUsers.associateBy { it.id }

                for (p in participants) {
                    val existing = existingUserMap[p.userId]
                    if (existing != null && p.displayName.isNullOrBlank()) {
                        continue
                    }
                    userDao.upsert(p.toUserEntity())
                }
            }
        }
    }

    private fun getCurrentUserId(): String? {
        cachedCurrentUserId?.let { return it }

        val fromPrefs = try {
            encryptedPrefs.getString(KEY_CURRENT_USER_ID, null)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to read current user id from prefs", e)
            null
        }
        if (fromPrefs != null) {
            cachedCurrentUserId = fromPrefs
            return fromPrefs
        }

        val fromJwt = extractUserIdFromJwt()
        if (fromJwt != null) {
            cachedCurrentUserId = fromJwt
            try {
                encryptedPrefs.edit()
                    .putString(KEY_CURRENT_USER_ID, fromJwt)
                    .apply()
            } catch (_: Exception) { /* best-effort persist */ }
        }
        return fromJwt
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

    companion object {
        private const val TAG = "ChatRepository"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}

// ── Local mapper extensions (duplicated from :app module) ────────────────

private fun String.toEpochMillisOrNull(): Long? = try {
    java.time.Instant.parse(this).toEpochMilli()
} catch (_: Exception) {
    null
}

private fun ChatDto.toEntity(): ChatEntity = ChatEntity(
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
    createdAt = System.currentTimeMillis(),
    updatedAt = updatedAt?.toEpochMillisOrNull() ?: System.currentTimeMillis()
)

private fun ChatParticipantDto.toEntity(chatId: String): ChatParticipantEntity =
    ChatParticipantEntity(
        chatId = chatId,
        userId = userId,
        role = role,
        joinedAt = System.currentTimeMillis()
    )

private fun ChatParticipantDto.toUserEntity(): UserEntity = UserEntity(
    id = userId,
    phone = "",
    displayName = displayName ?: userId.take(8),
    avatarUrl = avatarUrl,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
