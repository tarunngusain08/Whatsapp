package com.whatsappclone.core.database.relation

data class ContactWithUser(
    val contactId: String,
    val phone: String,
    val deviceName: String,
    val userId: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val statusText: String?,
    val isOnline: Boolean?
)
