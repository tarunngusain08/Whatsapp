package com.whatsappclone.feature.chat.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.network.websocket.TypingStateHolder
import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.domain.GetChatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatListNavigationEvent {
    data class NavigateToChat(val chatId: String) : ChatListNavigationEvent()
    data object NavigateToContactPicker : ChatListNavigationEvent()
    data object NavigateToSettings : ChatListNavigationEvent()
    data object NavigateToServerUrl : ChatListNavigationEvent()
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val chatRepository: ChatRepository,
    private val typingStateHolder: TypingStateHolder
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _loadingState = MutableStateFlow(LoadingState())

    private val _navigationEvent = MutableSharedFlow<ChatListNavigationEvent>()
    val navigationEvent: SharedFlow<ChatListNavigationEvent> = _navigationEvent.asSharedFlow()

    val uiState: StateFlow<ChatListUiState> = combine(
        getChatsUseCase(),
        typingStateHolder.typingUsers,
        _searchQuery,
        _loadingState
    ) { chats, typingMap, query, loading ->
        val enrichedChats = chats.map { chat ->
            val typingInChat = typingMap[chat.chatId] ?: emptySet()
            if (typingInChat.isNotEmpty()) {
                chat.copy(typingUsers = typingInChat)
            } else {
                chat
            }
        }

        val sortedChats = enrichedChats.sortedWith(
            compareByDescending<com.whatsappclone.feature.chat.model.ChatItemUi> { it.isPinned }
                .thenByDescending { it.lastMessageTimestamp ?: 0L }
        )

        ChatListUiState(
            chats = sortedChats,
            searchQuery = query,
            isLoading = loading.isLoading,
            isRefreshing = loading.isRefreshing,
            error = loading.error
        )
    }.catch { throwable ->
        emit(
            ChatListUiState(
                error = throwable.message ?: "Failed to load chats"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatListUiState(isLoading = true)
    )

    init {
        syncChats()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onRefresh() {
        _loadingState.update { it.copy(isRefreshing = true, error = null) }
        syncChats(isRefresh = true)
    }

    fun onChatClicked(chatId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(ChatListNavigationEvent.NavigateToChat(chatId))
        }
    }

    fun onNewChatClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(ChatListNavigationEvent.NavigateToContactPicker)
        }
    }

    fun onSettingsClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(ChatListNavigationEvent.NavigateToSettings)
        }
    }

    fun onServerUrlClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(ChatListNavigationEvent.NavigateToServerUrl)
        }
    }

    fun clearError() {
        _loadingState.update { it.copy(error = null) }
    }

    private fun syncChats(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _loadingState.update { it.copy(isLoading = true) }
            }

            when (val result = chatRepository.syncChats()) {
                is AppResult.Success -> {
                    _loadingState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = null)
                    }
                }

                is AppResult.Error -> {
                    _loadingState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message
                        )
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    private data class LoadingState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null
    )
}
