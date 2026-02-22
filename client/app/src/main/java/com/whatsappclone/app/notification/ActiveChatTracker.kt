package com.whatsappclone.app.notification

import com.whatsappclone.core.common.notification.ActiveChatTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveChatTrackerImpl @Inject constructor() : ActiveChatTracker {

    private val _activeChatId = MutableStateFlow<String?>(null)

    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    override fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    override fun isActive(chatId: String): Boolean = _activeChatId.value == chatId
}
