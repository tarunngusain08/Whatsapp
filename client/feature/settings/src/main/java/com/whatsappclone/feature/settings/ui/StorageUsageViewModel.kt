package com.whatsappclone.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.MessageDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUsageUiState(
    val chatStorageList: List<ChatStorageInfo> = emptyList(),
    val totalSize: Long = 0L,
    val totalMessages: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StorageUsageViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUsageUiState())
    val uiState: StateFlow<StorageUsageUiState> = _uiState.asStateFlow()

    init {
        loadStorageUsage()
    }

    private fun loadStorageUsage() {
        viewModelScope.launch {
            val chats = chatDao.getAllChats()
            val storageInfos = chats.map { chat ->
                val mediaSize = messageDao.getMediaSizeForChat(chat.chatId)
                val messageCount = messageDao.getMessageCountForChat(chat.chatId)
                ChatStorageInfo(
                    chatId = chat.chatId,
                    chatName = chat.name ?: "Unknown",
                    avatarUrl = chat.avatarUrl,
                    messageCount = messageCount,
                    mediaSize = mediaSize
                )
            }.sortedByDescending { it.mediaSize }

            _uiState.value = StorageUsageUiState(
                chatStorageList = storageInfos,
                totalSize = storageInfos.sumOf { it.mediaSize },
                totalMessages = storageInfos.sumOf { it.messageCount },
                isLoading = false
            )
        }
    }
}
