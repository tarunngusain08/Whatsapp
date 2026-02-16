package com.whatsappclone.core.ui.model

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ;

    companion object {
        fun fromString(status: String): MessageStatus = when (status.lowercase()) {
            "pending" -> PENDING
            "sent" -> SENT
            "delivered" -> DELIVERED
            "read" -> READ
            else -> PENDING
        }
    }
}
