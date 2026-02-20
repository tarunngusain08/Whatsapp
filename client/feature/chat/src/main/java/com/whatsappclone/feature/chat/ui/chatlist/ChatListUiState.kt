package com.whatsappclone.feature.chat.ui.chatlist

import com.whatsappclone.feature.chat.model.ChatItemUi

data class ChatListUiState(
    val chats: List<ChatItemUi> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val archivedCount: Int = 0
) {
    val filteredChats: List<ChatItemUi> by lazy {
        if (searchQuery.isBlank()) chats
        else chats.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.lastMessagePreview?.contains(searchQuery, ignoreCase = true) == true
        }
    }
}
