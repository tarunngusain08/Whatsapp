package com.whatsappclone.feature.contacts.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.feature.chat.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactInfoUiState(
    val user: UserEntity? = null,
    val chatId: String? = null,
    val isLoading: Boolean = true,
    val isMuted: Boolean = false,
    val isBlocking: Boolean = false,
    val error: String? = null
)

sealed class ContactInfoEvent {
    data class NavigateToChat(val userId: String) : ContactInfoEvent()
    data object NavigateBack : ContactInfoEvent()
}

@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(ContactInfoUiState())
    val uiState: StateFlow<ContactInfoUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ContactInfoEvent>()
    val event: SharedFlow<ContactInfoEvent> = _event.asSharedFlow()

    init {
        loadUser()
        observeUser()
        resolveChatId()
    }

    private fun resolveChatId() {
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserId() ?: return@launch
            val chatId = chatDao.findDirectChatWithUser(currentUserId, userId)
            if (chatId != null) {
                _uiState.update { it.copy(chatId = chatId) }
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            when (val result = userRepository.getUser(userId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(user = result.data, isLoading = false)
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    private fun observeUser() {
        viewModelScope.launch {
            userRepository.observeUser(userId)
                .filterNotNull()
                .collect { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                }
        }
    }

    fun onMessageClicked() {
        viewModelScope.launch {
            val chatId = _uiState.value.chatId ?: userId
            _event.emit(ContactInfoEvent.NavigateToChat(chatId))
        }
    }

    fun onMuteToggled() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun onBlockClicked() {
        val currentUser = _uiState.value.user ?: return
        if (_uiState.value.isBlocking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBlocking = true, error = null) }

            val result = if (currentUser.isBlocked) {
                userRepository.unblockUser(userId)
            } else {
                userRepository.blockUser(userId)
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isBlocking = false) }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isBlocking = false, error = result.message)
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _event.emit(ContactInfoEvent.NavigateBack)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
