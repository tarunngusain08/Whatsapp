package com.whatsappclone.feature.chat.data

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun getMe(): AppResult<UserEntity>

    suspend fun updateProfile(
        displayName: String?,
        statusText: String?
    ): AppResult<UserEntity>

    fun observeUser(userId: String): Flow<UserEntity?>

    suspend fun getUser(userId: String): AppResult<UserEntity>

    suspend fun blockUser(userId: String): AppResult<Unit>

    suspend fun unblockUser(userId: String): AppResult<Unit>

    /**
     * Returns the locally-cached current user id, or null if not yet fetched.
     */
    fun getCurrentUserId(): String?
}
