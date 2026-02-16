package com.whatsappclone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["phone"], unique = true)]
)
data class ContactEntity(
    @PrimaryKey
    val contactId: String,
    val phone: String,
    val deviceName: String,
    val registeredUserId: String? = null,
    val updatedAt: Long
)
