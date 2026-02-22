package com.whatsappclone.core.common.notification

/**
 * Tracks which chat the user currently has open so that incoming push
 * notifications for that chat can be suppressed.
 */
interface ActiveChatTracker {

    fun setActiveChat(chatId: String?)

    fun isActive(chatId: String): Boolean
}
