package com.whatsappclone.feature.chat.ui.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StarredMessageUi(
    val messageId: String,
    val chatId: String,
    val senderName: String,
    val content: String?,
    val messageType: String,
    val formattedTime: String,
    val timestamp: Long
)

data class StarredMessagesUiState(
    val messages: List<StarredMessageUi> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StarredMessagesViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarredMessagesUiState())
    val uiState: StateFlow<StarredMessagesUiState> = _uiState.asStateFlow()

    init {
        observeStarred()
    }

    private fun observeStarred() {
        viewModelScope.launch {
            messageDao.observeStarredMessages().collect { entities ->
                val uiMessages = entities.map { it.toUi() }
                _uiState.update { it.copy(messages = uiMessages, isLoading = false) }
            }
        }
    }

    private suspend fun MessageEntity.toUi(): StarredMessageUi {
        val user = userDao.getById(senderId)
        val senderName = user?.displayName ?: senderId.take(8)
        val formatter = java.text.SimpleDateFormat(
            "MMM d, h:mm a",
            java.util.Locale.getDefault()
        )
        return StarredMessageUi(
            messageId = messageId,
            chatId = chatId,
            senderName = senderName,
            content = content,
            messageType = messageType,
            formattedTime = formatter.format(java.util.Date(timestamp)),
            timestamp = timestamp
        )
    }
}
