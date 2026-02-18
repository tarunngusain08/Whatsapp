package com.whatsappclone.feature.chat.data

import android.content.SharedPreferences
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.core.network.api.UserApi
import com.whatsappclone.core.network.model.dto.UpdateProfileRequest
import com.whatsappclone.core.network.model.dto.UserDto
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.core.network.model.safeApiCallUnit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) : UserRepository {

    companion object {
        private const val TAG = "UserRepository"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    // ── Get current user ─────────────────────────────────────────────────

    override suspend fun getMe(): AppResult<UserEntity> {
        val result = safeApiCall { userApi.getMe() }
        return when (result) {
            is AppResult.Success -> {
                val entity = result.data.toEntity()
                userDao.upsert(entity)
                saveCurrentUserId(entity.id)
                AppResult.Success(entity)
            }
            is AppResult.Error -> {
                val cachedId = getCurrentUserId()
                if (cachedId != null) {
                    val cached = userDao.getById(cachedId)
                    if (cached != null) return AppResult.Success(cached)
                }
                result
            }
            is AppResult.Loading -> AppResult.Error(
                code = ErrorCode.UNKNOWN,
                message = "Unexpected loading state"
            )
        }
    }

    // ── Update profile ───────────────────────────────────────────────────

    override suspend fun updateProfile(
        displayName: String?,
        statusText: String?
    ): AppResult<UserEntity> {
        val request = UpdateProfileRequest(
            displayName = displayName,
            statusText = statusText
        )

        val result = safeApiCall { userApi.updateProfile(request) }
        return when (result) {
            is AppResult.Success -> {
                val entity = result.data.toEntity()
                userDao.upsert(entity)
                AppResult.Success(entity)
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Error(
                code = ErrorCode.UNKNOWN,
                message = "Unexpected loading state"
            )
        }
    }

    // ── Observe ──────────────────────────────────────────────────────────

    override fun observeUser(userId: String): Flow<UserEntity?> =
        userDao.observeUser(userId)

    // ── Get single user ──────────────────────────────────────────────────

    override suspend fun getUser(userId: String): AppResult<UserEntity> {
        val cached = userDao.getById(userId)
        if (cached != null) return AppResult.Success(cached)

        val result = safeApiCall { userApi.getUser(userId) }
        return when (result) {
            is AppResult.Success -> {
                val entity = result.data.toEntity()
                userDao.upsert(entity)
                AppResult.Success(entity)
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Error(
                code = ErrorCode.UNKNOWN,
                message = "Unexpected loading state"
            )
        }
    }

    // ── Block / Unblock ──────────────────────────────────────────────────

    override suspend fun blockUser(userId: String): AppResult<Unit> {
        userDao.setBlocked(userId, isBlocked = true, updatedAt = System.currentTimeMillis())

        val result = safeApiCallUnit { userApi.blockUser(userId) }
        if (result is AppResult.Error) {
            userDao.setBlocked(userId, isBlocked = false, updatedAt = System.currentTimeMillis())
        }
        return result
    }

    override suspend fun unblockUser(userId: String): AppResult<Unit> {
        userDao.setBlocked(userId, isBlocked = false, updatedAt = System.currentTimeMillis())

        val result = safeApiCallUnit { userApi.unblockUser(userId) }
        if (result is AppResult.Error) {
            userDao.setBlocked(userId, isBlocked = true, updatedAt = System.currentTimeMillis())
        }
        return result
    }

    // ── Current user id ──────────────────────────────────────────────────

    override fun getCurrentUserId(): String? {
        return try {
            encryptedPrefs.getString(KEY_CURRENT_USER_ID, null)?.let { return it }
            extractUserIdFromJwt()?.also { userId ->
                saveCurrentUserId(userId)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to read current user id from prefs", e)
            extractUserIdFromJwt()
        }
    }

    private fun saveCurrentUserId(userId: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_CURRENT_USER_ID, userId)
                .apply()
        } catch (_: Exception) { /* best-effort cache */ }
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
}

// ── Local mapper extension (duplicated from :app module) ─────────────────

private fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    phone = phone,
    displayName = displayName,
    statusText = statusText,
    avatarUrl = avatarUrl,
    isOnline = isOnline ?: false,
    lastSeen = null,
    isBlocked = false,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
