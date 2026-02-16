package com.whatsappclone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["phone"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val phone: String,
    val displayName: String,
    val statusText: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val isBlocked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
