package com.whatsappclone.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatParticipantDao {

    @Query("SELECT * FROM chat_participants WHERE chatId = :chatId ORDER BY joinedAt ASC")
    fun observeParticipants(chatId: String): Flow<List<ChatParticipantEntity>>

    @Query("SELECT * FROM chat_participants WHERE chatId = :chatId ORDER BY joinedAt ASC")
    suspend fun getParticipants(chatId: String): List<ChatParticipantEntity>

    @Upsert
    suspend fun upsert(participant: ChatParticipantEntity)

    @Upsert
    suspend fun upsertAll(participants: List<ChatParticipantEntity>)

    @Query("DELETE FROM chat_participants WHERE chatId = :chatId AND userId = :userId")
    suspend fun delete(chatId: String, userId: String)

    @Query("DELETE FROM chat_participants WHERE chatId = :chatId")
    suspend fun deleteAllForChat(chatId: String)
}
