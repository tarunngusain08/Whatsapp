package com.whatsappclone.feature.chat.ui.forward

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.relation.ChatWithLastMessage
import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.data.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForwardPickerUiState(
    val chats: List<ChatWithLastMessage> = emptyList(),
    val selectedChatIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isForwarding: Boolean = false,
    val forwardComplete: Boolean = false,
    val error: String? = null
) {
    val filteredChats: List<ChatWithLastMessage>
        get() = if (searchQuery.isBlank()) {
            chats
        } else {
            chats.filter { chat ->
                chat.chat.name?.contains(searchQuery, ignoreCase = true) == true
            }
        }

    val selectedCount: Int get() = selectedChatIds.size
}

@HiltViewModel
class ForwardPickerViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForwardPickerUiState())
    val uiState: StateFlow<ForwardPickerUiState> = _uiState.asStateFlow()

    init {
        observeChats()
    }

    private fun observeChats() {
        viewModelScope.launch {
            chatRepository.observeChats().collect { chats ->
                _uiState.update { it.copy(chats = chats) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleChatSelection(chatId: String) {
        _uiState.update { state ->
            val updated = state.selectedChatIds.toMutableSet()
            if (chatId in updated) {
                updated.remove(chatId)
            } else {
                updated.add(chatId)
            }
            state.copy(selectedChatIds = updated)
        }
    }

    fun forwardMessage(
        originalContent: String?,
        originalMessageType: String
    ) {
        val selectedIds = _uiState.value.selectedChatIds.toList()
        if (selectedIds.isEmpty() || originalContent.isNullOrBlank()) return

        _uiState.update { it.copy(isForwarding = true) }

        viewModelScope.launch {
            var hasError = false
            for (chatId in selectedIds) {
                val result = messageRepository.saveAndSend(
                    chatId = chatId,
                    content = originalContent,
                    messageType = originalMessageType
                )
                if (result is AppResult.Error) {
                    hasError = true
                }
            }

            _uiState.update {
                it.copy(
                    isForwarding = false,
                    forwardComplete = !hasError,
                    error = if (hasError) "Some messages failed to forward" else null
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
