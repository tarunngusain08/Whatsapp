package com.whatsappclone.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val chatId: String,
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val createdBy: String,
    val isAdminOnly: Boolean = false,
    val memberCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
