package com.whatsappclone.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.whatsappclone.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query(
        """
        SELECT * FROM messages 
        WHERE chatId = :chatId AND isDeleted = 0 
        ORDER BY timestamp DESC
        """
    )
    fun pagingSource(chatId: String): PagingSource<Int, MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE chatId = :chatId AND isDeleted = 0 
        ORDER BY timestamp DESC 
        LIMIT :limit
        """
    )
    fun observeRecentMessages(chatId: String, limit: Int = 50): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE clientMsgId = :clientMsgId")
    suspend fun getByClientMsgId(clientMsgId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<MessageEntity>): List<Long>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query(
        """
        UPDATE messages 
        SET messageId = :serverMessageId, status = 'sent' 
        WHERE clientMsgId = :clientMsgId AND status IN ('pending', 'sending')
        """
    )
    suspend fun confirmSent(clientMsgId: String, serverMessageId: String)

    @Query(
        """
        UPDATE messages 
        SET isDeleted = 1, deletedForEveryone = :forEveryone, content = NULL 
        WHERE messageId = :messageId
        """
    )
    suspend fun softDelete(messageId: String, forEveryone: Boolean)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE messageId = :messageId")
    suspend fun setStarred(messageId: String, isStarred: Boolean)

    @Query("SELECT * FROM messages WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getAllPendingMessages(): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE chatId = :chatId 
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAllForChat(chatId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteAllForChat(chatId: String)

    /**
     * Full-text search across all messages. Returns messages whose content
     * matches the given FTS query, ordered by most recent first.
     */
    @Query(
        """
        SELECT m.* FROM messages m
        JOIN messages_fts fts ON m.rowid = fts.rowid
        WHERE messages_fts MATCH :query
          AND m.isDeleted = 0
        ORDER BY m.timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun searchMessages(query: String, limit: Int = 50): List<MessageEntity>

    /**
     * Full-text search within a specific chat.
     */
    @Query(
        """
        SELECT m.* FROM messages m
        JOIN messages_fts fts ON m.rowid = fts.rowid
        WHERE messages_fts MATCH :query
          AND m.chatId = :chatId
          AND m.isDeleted = 0
        ORDER BY m.timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun searchMessagesInChat(
        chatId: String,
        query: String,
        limit: Int = 100
    ): List<MessageEntity>
}
