package com.whatsappclone.feature.chat.ui.chatdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.common.util.TimeUtils
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.network.websocket.TypingStateHolder
import com.whatsappclone.core.network.websocket.WebSocketManager
import com.whatsappclone.core.network.websocket.WsConnectionState
import com.whatsappclone.core.network.websocket.WsFrame
import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.data.MessageRepository
import com.whatsappclone.feature.chat.data.UserRepository
import com.whatsappclone.feature.chat.domain.MarkMessagesReadUseCase
import com.whatsappclone.feature.chat.domain.SendMessageUseCase
import com.whatsappclone.feature.chat.model.MessageUi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markMessagesReadUseCase: MarkMessagesReadUseCase,
    private val webSocketManager: WebSocketManager,
    private val typingStateHolder: TypingStateHolder,
    private val messageDao: MessageDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val userDao: UserDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val currentUserId: String = userRepository.getCurrentUserId() ?: ""

    private val _uiState = MutableStateFlow(ChatDetailUiState(chatId = chatId, isLoading = true))
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<PagingData<MessageUi>>(PagingData.empty())
    val messages: StateFlow<PagingData<MessageUi>> = _messages.asStateFlow()

    private val _replyToMessage = MutableStateFlow<MessageUi?>(null)
    val replyToMessage: StateFlow<MessageUi?> = _replyToMessage.asStateFlow()

    private var typingJob: Job? = null
    private var isTypingSent = false

    init {
        loadChatDetail()
        observeMessages()
        observeTypingIndicators()
    }

    // ── Chat metadata ───────────────────────────────────────────────────

    private fun loadChatDetail() {
        viewModelScope.launch {
            when (val result = chatRepository.getChatDetail(chatId)) {
                is AppResult.Success -> {
                    val chat = result.data
                    var chatName = chat.name ?: "Chat"
                    var chatAvatar = chat.avatarUrl

                    if (chat.chatType == "direct" && currentUserId.isNotBlank()) {
                        try {
                            val participants = chatParticipantDao.getParticipants(chatId)
                            val otherUserId = participants
                                .firstOrNull { it.userId != currentUserId }?.userId
                            if (otherUserId != null) {
                                val otherUser = userDao.getById(otherUserId)
                                if (otherUser != null) {
                                    chatName = otherUser.displayName
                                    chatAvatar = otherUser.avatarUrl ?: chatAvatar
                                }
                            }
                        } catch (_: Exception) { }
                    }

                    _uiState.update {
                        it.copy(
                            chatName = chatName,
                            chatAvatarUrl = chatAvatar,
                            chatType = chat.chatType,
                            isLoading = false
                        )
                    }
                    if (chat.chatType == "direct") {
                        observeOtherUserPresence()
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

    private fun observeOtherUserPresence() {
        viewModelScope.launch {
            // For a direct chat the "other user" id is typically embedded in
            // participants. Simplified approach: observe via chatId.
        }
    }

    fun updateOtherUserPresence(isOnline: Boolean, lastSeenMillis: Long?) {
        _uiState.update {
            it.copy(
                isOnline = isOnline,
                lastSeen = lastSeenMillis?.let { ms -> TimeUtils.formatLastSeen(ms) }
            )
        }
    }

    // ── Messages ────────────────────────────────────────────────────────

    private fun observeMessages() {
        viewModelScope.launch {
            messageRepository.observeMessages(chatId)
                .collectLatest { pagingData ->
                    _messages.value = pagingData.map { entity ->
                        entity.toMessageUi()
                    }
                }
        }
    }

    private suspend fun MessageEntity.toMessageUi(): MessageUi {
        // Resolve reply-to details if this message is a reply
        var replyContent: String? = null
        var replySenderName: String? = null
        var replyType: String? = null
        var replyMediaThumb: String? = null

        val replyId = replyToMessageId
        if (replyId != null) {
            val replyEntity = messageDao.getById(replyId)
            if (replyEntity != null) {
                replyContent = replyEntity.content
                replyType = replyEntity.messageType
                replyMediaThumb = replyEntity.mediaThumbnailUrl
                replySenderName = if (replyEntity.senderId == currentUserId) {
                    "You"
                } else {
                    // Try to get the user's display name
                    val userResult = userRepository.getUser(replyEntity.senderId)
                    if (userResult is AppResult.Success) {
                        userResult.data.displayName ?: replyEntity.senderId.take(8)
                    } else {
                        replyEntity.senderId.take(8)
                    }
                }
            }
        }

        return MessageUi(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            senderName = null,
            content = content,
            messageType = messageType,
            status = status,
            isOwnMessage = senderId == currentUserId,
            isDeleted = isDeleted,
            isStarred = isStarred,
            mediaUrl = mediaUrl,
            mediaThumbnailUrl = mediaThumbnailUrl,
            replyToMessageId = replyToMessageId,
            formattedTime = TimeUtils.formatMessageTime(timestamp),
            timestamp = timestamp,
            replyToContent = replyContent,
            replyToSenderName = replySenderName,
            replyToType = replyType,
            replyToMediaThumbnailUrl = replyMediaThumb
        )
    }

    // ── Typing indicators ───────────────────────────────────────────────

    private fun observeTypingIndicators() {
        viewModelScope.launch {
            typingStateHolder.getTypingUsersForChat(chatId)
                .distinctUntilChanged()
                .collect { userIds ->
                    _uiState.update { it.copy(typingUsers = userIds) }
                }
        }
    }

    // ── Composer ─────────────────────────────────────────────────────────

    fun onComposerTextChanged(text: String) {
        if (text.length > Constants.MAX_MESSAGE_LENGTH) return

        _uiState.update { it.copy(composerText = text) }
        sendTypingStart()
    }

    private fun sendTypingStart() {
        if (isTypingSent) return

        if (webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
            webSocketManager.sendTyping(chatId, isTyping = true)
            isTypingSent = true
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(Constants.TYPING_DEBOUNCE_MS)
            if (webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
                webSocketManager.sendTyping(chatId, isTyping = false)
            }
            isTypingSent = false
        }
    }

    fun onSendMessage() {
        val text = _uiState.value.composerText.trim()
        if (text.isBlank()) return

        val replyId = _replyToMessage.value?.messageId

        _uiState.update { it.copy(composerText = "") }
        _replyToMessage.value = null

        // Cancel any outgoing typing indicator
        typingJob?.cancel()
        if (isTypingSent && webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
            webSocketManager.sendTyping(chatId, isTyping = false)
        }
        isTypingSent = false

        viewModelScope.launch {
            when (val result = sendMessageUseCase(chatId, text, replyToMessageId = replyId)) {
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> Unit
            }
        }
    }

    // ── Reply ───────────────────────────────────────────────────────────

    fun setReplyTo(message: MessageUi?) {
        _replyToMessage.value = message
    }

    fun clearReply() {
        _replyToMessage.value = null
    }

    // ── Star / Unstar ───────────────────────────────────────────────────

    fun toggleStar(messageId: String, isCurrentlyStarred: Boolean) {
        viewModelScope.launch {
            val result = messageRepository.starMessage(messageId, !isCurrentlyStarred)
            if (result is AppResult.Error) {
                _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    // ── Delete — For Me ─────────────────────────────────────────────────

    fun deleteForMe(messageId: String) {
        viewModelScope.launch {
            messageRepository.softDelete(messageId, forEveryone = false)
        }
    }

    // ── Delete — For Everyone ───────────────────────────────────────────

    fun deleteForEveryone(messageId: String) {
        viewModelScope.launch {
            // Update local DB
            messageRepository.softDelete(messageId, forEveryone = true)

            // Send WS event to notify other participants
            if (webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
                val data = buildJsonObject {
                    put("message_id", JsonPrimitive(messageId))
                    put("chat_id", JsonPrimitive(chatId))
                    put("deleted_for_everyone", JsonPrimitive(true))
                }
                webSocketManager.send(WsFrame(event = "message.deleted", data = data))
            }
        }
    }

    // ── Copy to clipboard ───────────────────────────────────────────────

    fun copyToClipboard(content: String) {
        val clipboardManager = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", content)
        clipboardManager.setPrimaryClip(clip)
    }

    // ── Read receipts ───────────────────────────────────────────────────

    fun markAsRead() {
        viewModelScope.launch {
            markMessagesReadUseCase(chatId)
        }
    }

    // ── In-chat search ─────────────────────────────────────────────────

    private var searchJob: Job? = null
    private val searchQueryFlow = MutableStateFlow("")

    fun openSearch() {
        _uiState.update { it.copy(isSearchActive = true, searchQuery = "") }
    }

    fun closeSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                isSearchActive = false,
                searchQuery = "",
                matchedMessageIds = emptyList(),
                currentMatchIndex = 0
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.length < 2) {
            _uiState.update {
                it.copy(matchedMessageIds = emptyList(), currentMatchIndex = 0)
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performInChatSearch(query.trim())
        }
    }

    private suspend fun performInChatSearch(query: String) {
        try {
            val ftsQuery = "\"${query.replace("\"", "")}*\""
            val results = messageDao.searchMessagesInChat(chatId, ftsQuery)
            val ids = results.map { it.messageId }

            _uiState.update {
                it.copy(
                    matchedMessageIds = ids,
                    currentMatchIndex = if (ids.isNotEmpty()) 0 else 0
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(matchedMessageIds = emptyList(), currentMatchIndex = 0)
            }
        }
    }

    fun navigateToNextMatch() {
        val state = _uiState.value
        if (state.matchedMessageIds.isEmpty()) return

        val nextIndex = (state.currentMatchIndex + 1) % state.matchedMessageIds.size
        _uiState.update { it.copy(currentMatchIndex = nextIndex) }
    }

    fun navigateToPreviousMatch() {
        val state = _uiState.value
        if (state.matchedMessageIds.isEmpty()) return

        val prevIndex = if (state.currentMatchIndex > 0) {
            state.currentMatchIndex - 1
        } else {
            state.matchedMessageIds.size - 1
        }
        _uiState.update { it.copy(currentMatchIndex = prevIndex) }
    }

    // ── Error handling ──────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
