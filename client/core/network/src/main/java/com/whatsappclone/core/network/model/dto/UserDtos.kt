package com.whatsappclone.core.network.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val phone: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("status_text")
    val statusText: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("is_online")
    val isOnline: Boolean? = null,
    @SerialName("last_seen")
    val lastSeen: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("status_text")
    val statusText: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

@Serializable
data class PresenceDto(
    @SerialName("user_id")
    val userId: String,
    val online: Boolean,
    @SerialName("last_seen")
    val lastSeen: String? = null
)

@Serializable
data class ContactSyncRequest(
    @SerialName("phone_numbers")
    val phoneNumbers: List<String>
)

@Serializable
data class ContactSyncResponse(
    @SerialName("registered_users")
    val registeredUsers: List<ContactSyncUserDto>
)

/**
 * Matches the backend ContactSyncResult shape which uses `user_id`
 * instead of `id` and only includes a subset of user fields.
 */
@Serializable
data class ContactSyncUserDto(
    @SerialName("user_id")
    val id: String,
    val phone: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("status_text")
    val statusText: String? = null,
    @SerialName("is_online")
    val isOnline: Boolean? = null
)
