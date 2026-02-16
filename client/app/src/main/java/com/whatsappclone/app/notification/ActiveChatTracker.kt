package com.whatsappclone.app.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks which chat the user currently has open so that incoming push
 * notifications for that chat can be suppressed (no system notification
 * while the user is already reading the conversation).
 *
 * Call [setActiveChat] when a chat screen is entered/exited.
 */
@Singleton
class ActiveChatTracker @Inject constructor() {

    private val _activeChatId = MutableStateFlow<String?>(null)

    /** The chatId of the currently visible chat screen, or null when none is open. */
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    /**
     * Sets the active chat. Pass `null` when the user leaves the chat screen.
     */
    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    /**
     * Returns true if the given [chatId] is the one currently open on screen.
     */
    fun isActive(chatId: String): Boolean = _activeChatId.value == chatId
}
