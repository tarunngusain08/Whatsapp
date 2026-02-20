package com.whatsappclone.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.whatsappclone.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id IN (:userIds)")
    suspend fun getByIds(userIds: List<String>): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE phone = :phone")
    suspend fun getByPhone(phone: String): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updatePresence(userId: String, isOnline: Boolean, lastSeen: Long?, updatedAt: Long)

    @Query("UPDATE users SET isBlocked = :isBlocked, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun setBlocked(userId: String, isBlocked: Boolean, updatedAt: Long)

    @Query("SELECT * FROM users WHERE isBlocked = 1 ORDER BY displayName ASC")
    fun observeBlockedUsers(): Flow<List<UserEntity>>
}
