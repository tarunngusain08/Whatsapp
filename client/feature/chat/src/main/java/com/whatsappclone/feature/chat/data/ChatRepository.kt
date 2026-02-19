package com.whatsappclone.feature.chat.data

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.relation.ChatWithLastMessage
import com.whatsappclone.core.network.model.dto.ChatDto
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun observeChats(currentUserId: String? = null): Flow<List<ChatWithLastMessage>>

    suspend fun syncChats(): AppResult<Unit>

    /**
     * Creates a direct (1:1) chat with [otherUserId].
     * Returns the chatId — either existing or newly created.
     */
    suspend fun createDirectChat(otherUserId: String): AppResult<String>

    suspend fun getChatDetail(chatId: String): AppResult<ChatEntity>

    suspend fun updateLastMessage(
        chatId: String,
        messageId: String,
        preview: String?,
        timestamp: Long
    )

    suspend fun incrementUnreadCount(chatId: String)

    suspend fun resetUnreadCount(chatId: String)

    suspend fun muteChat(chatId: String, muted: Boolean): AppResult<Unit>

    suspend fun pinChat(chatId: String, pinned: Boolean)

    /**
     * Creates a group chat with the given [name] and [participantIds].
     * Returns the chatId of the newly created group.
     */
    suspend fun createGroupChat(name: String, participantIds: List<String>): AppResult<String>

    /**
     * Inserts or updates a chat received from a remote source (WebSocket / push).
     */
    suspend fun insertFromRemote(chatDto: ChatDto)

    suspend fun setDisappearingTimer(chatId: String, timer: String): AppResult<Unit>

    suspend fun archiveChat(chatId: String, archived: Boolean)

    suspend fun deleteChat(chatId: String)

    fun observeArchivedChats(currentUserId: String? = null): Flow<List<ChatWithLastMessage>>

    fun observeArchivedCount(): Flow<Int>
}
