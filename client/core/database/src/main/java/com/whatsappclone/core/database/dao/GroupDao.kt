package com.whatsappclone.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.whatsappclone.core.database.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM `groups` WHERE chatId = :chatId")
    suspend fun getByChatId(chatId: String): GroupEntity?

    @Query("SELECT * FROM `groups` WHERE chatId = :chatId")
    fun observeGroup(chatId: String): Flow<GroupEntity?>

    @Upsert
    suspend fun upsert(group: GroupEntity)
}
