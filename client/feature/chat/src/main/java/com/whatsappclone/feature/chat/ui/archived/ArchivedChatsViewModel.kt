package com.whatsappclone.feature.chat.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.domain.GetChatsUseCase
import com.whatsappclone.feature.chat.model.ChatItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchivedChatsUiState(
    val chats: List<ChatItemUi> = emptyList()
)

@HiltViewModel
class ArchivedChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val uiState: StateFlow<ArchivedChatsUiState> = chatRepository.observeArchivedChats()
        .map { chatsWithMsg ->
            val items = chatsWithMsg.map { cwm ->
                val resolvedName = if (cwm.chat.chatType == "direct") {
                    cwm.directChatOtherUserName ?: cwm.chat.name ?: "Unknown"
                } else {
                    cwm.chat.name ?: "Group"
                }
                val resolvedAvatar = if (cwm.chat.chatType == "direct") {
                    cwm.directChatOtherUserAvatarUrl ?: cwm.chat.avatarUrl
                } else {
                    cwm.chat.avatarUrl
                }
                ChatItemUi(
                    chatId = cwm.chat.chatId,
                    name = resolvedName,
                    avatarUrl = resolvedAvatar,
                    lastMessagePreview = cwm.lastMessageText,
                    lastMessageTimestamp = cwm.chat.lastMessageTimestamp,
                    lastMessageSenderName = cwm.lastMessageSenderName,
                    unreadCount = cwm.chat.unreadCount,
                    isMuted = cwm.chat.isMuted,
                    isPinned = cwm.chat.isPinned,
                    chatType = cwm.chat.chatType,
                    isArchived = true
                )
            }
            ArchivedChatsUiState(chats = items)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArchivedChatsUiState()
        )

    fun unarchiveChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.archiveChat(chatId, false)
        }
    }
}
