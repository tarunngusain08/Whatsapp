package com.whatsappclone.feature.chat.data

import android.content.SharedPreferences
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.map
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatParticipantDao
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
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) : ChatRepository {

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

    // ── Remote insert ────────────────────────────────────────────────────

    override suspend fun insertFromRemote(chatDto: ChatDto) {
        upsertChatWithRelations(chatDto)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private suspend fun upsertChatWithRelations(chatDto: ChatDto) {
        chatDao.upsert(chatDto.toEntity())

        chatDto.participants?.let { participants ->
            val participantEntities = participants.map { it.toEntity(chatDto.chatId) }
            chatParticipantDao.upsertAll(participantEntities)

            val userEntities = participants.map { it.toUserEntity() }
            userDao.upsertAll(userEntities)
        }
    }

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
            android.util.Log.w(TAG, "Failed to read current user id from prefs", e)
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
    displayName = displayName ?: "Unknown",
    avatarUrl = avatarUrl,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
