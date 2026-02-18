package com.whatsappclone.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.relation.ChatWithLastMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        """
        SELECT 
            c.*,
            m.content AS lastMessageText,
            m.messageType AS lastMessageType,
            m.senderId AS lastMessageSenderId,
            u.displayName AS lastMessageSenderName,
            pu.displayName AS directChatOtherUserName,
            pu.avatarUrl AS directChatOtherUserAvatarUrl
        FROM chats c
        LEFT JOIN messages m ON c.lastMessageId = m.messageId
        LEFT JOIN users u ON m.senderId = u.id
        LEFT JOIN chat_participants cp 
            ON c.chatId = cp.chatId AND c.chatType = 'direct' AND cp.userId != :currentUserId
        LEFT JOIN users pu ON cp.userId = pu.id
        ORDER BY c.isPinned DESC, c.lastMessageTimestamp DESC
        """
    )
    fun observeChatsWithLastMessage(currentUserId: String): Flow<List<ChatWithLastMessage>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    @Upsert
    suspend fun upsert(chat: ChatEntity)

    @Upsert
    suspend fun upsertAll(chats: List<ChatEntity>)

    @Query("UPDATE chats SET unreadCount = :count, updatedAt = :updatedAt WHERE chatId = :chatId")
    suspend fun updateUnreadCount(chatId: String, count: Int, updatedAt: Long)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1, updatedAt = :updatedAt WHERE chatId = :chatId")
    suspend fun incrementUnreadCount(chatId: String, updatedAt: Long)

    @Query("UPDATE chats SET isMuted = :isMuted, updatedAt = :updatedAt WHERE chatId = :chatId")
    suspend fun setMuted(chatId: String, isMuted: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE chats 
        SET lastMessageId = :messageId, 
            lastMessagePreview = :preview, 
            lastMessageTimestamp = :timestamp, 
            updatedAt = :updatedAt 
        WHERE chatId = :chatId
        """
    )
    suspend fun updateLastMessage(
        chatId: String,
        messageId: String,
        preview: String?,
        timestamp: Long,
        updatedAt: Long
    )

    @Query(
        """
        SELECT c.chatId FROM chats c
        INNER JOIN chat_participants cp1 ON c.chatId = cp1.chatId
        INNER JOIN chat_participants cp2 ON c.chatId = cp2.chatId
        WHERE c.chatType = 'direct'
          AND cp1.userId = :currentUserId
          AND cp2.userId = :otherUserId
        LIMIT 1
        """
    )
    suspend fun findDirectChatWithUser(currentUserId: String, otherUserId: String): String?

    @Query("DELETE FROM chats")
    suspend fun deleteAll()
}
