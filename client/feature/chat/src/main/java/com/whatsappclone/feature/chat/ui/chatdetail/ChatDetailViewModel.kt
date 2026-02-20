package com.whatsappclone.feature.chat.ui.chatdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.common.util.TimeUtils
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.network.websocket.ServerWsEvent
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
import com.whatsappclone.feature.chat.model.Reaction
import com.whatsappclone.feature.media.audio.RecordingState
import com.whatsappclone.feature.media.audio.VoiceRecorder
import com.whatsappclone.feature.media.data.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
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
    private val mediaRepository: MediaRepository,
    private val voiceRecorder: VoiceRecorder,
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

    val recordingState: StateFlow<RecordingState> = voiceRecorder.state
    val recordingAmplitudes: SharedFlow<Int> = voiceRecorder.amplitudes

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var typingJob: Job? = null
    private var isTypingSent = false

    init {
        loadChatDetail()
        observeMessages()
        observeTypingIndicators()
        observeIncomingMessages()
    }

    // ── Chat metadata ───────────────────────────────────────────────────

    private fun loadChatDetail() {
        viewModelScope.launch {
            when (val result = chatRepository.getChatDetail(chatId)) {
                is AppResult.Success -> {
                    val chat = result.data
                    var chatName = chat.name ?: "Chat"
                    var chatAvatar = chat.avatarUrl
                    var resolvedOtherUserId: String? = null

                    if (chat.chatType == "direct" && currentUserId.isNotBlank()) {
                        try {
                            val participants = chatParticipantDao.getParticipants(chatId)
                            resolvedOtherUserId = participants
                                .firstOrNull { it.userId != currentUserId }?.userId
                            if (resolvedOtherUserId != null) {
                                val otherUser = userDao.getById(resolvedOtherUserId)
                                if (otherUser != null &&
                                    otherUser.displayName != "Unknown" &&
                                    otherUser.displayName.isNotBlank()
                                ) {
                                    chatName = otherUser.displayName
                                    chatAvatar = otherUser.avatarUrl ?: chatAvatar
                                } else {
                                    val fetched = userRepository.getUser(resolvedOtherUserId)
                                    if (fetched is AppResult.Success) {
                                        chatName = fetched.data.displayName
                                        chatAvatar = fetched.data.avatarUrl ?: chatAvatar
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }

                    _uiState.update {
                        it.copy(
                            chatName = chatName,
                            chatAvatarUrl = chatAvatar,
                            chatType = chat.chatType,
                            otherUserId = resolvedOtherUserId,
                            isMuted = chat.isMuted,
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
        val otherUserId = _uiState.value.otherUserId ?: return
        viewModelScope.launch {
            if (webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
                val data = buildJsonObject {
                    put("user_ids", kotlinx.serialization.json.buildJsonArray {
                        add(JsonPrimitive(otherUserId))
                    })
                }
                webSocketManager.send(WsFrame(event = "presence.subscribe", data = data))
            }

            userDao.observeUser(otherUserId)
                .distinctUntilChanged()
                .collect { user ->
                    _uiState.update {
                        it.copy(
                            isOnline = user?.isOnline ?: false,
                            lastSeen = user?.lastSeen?.let { ms ->
                                TimeUtils.formatLastSeen(ms)
                            }
                        )
                    }
                }
        }
    }

    // ── Messages ────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMessages() {
        viewModelScope.launch {
            merge(flowOf(Unit), refreshTrigger)
                .flatMapLatest {
                    messageRepository.observeMessages(chatId)
                        .cachedIn(viewModelScope)
                }
                .collectLatest { pagingData ->
                    _messages.value = pagingData.map { entity ->
                        entity.toMessageUi()
                    }
                }
        }
    }

    /**
     * Re-creates the Pager whenever a new WebSocket message arrives for
     * this chat, so the UI picks up inserts made by [WsEventRouter].
     */
    private fun observeIncomingMessages() {
        viewModelScope.launch {
            webSocketManager.events
                .filter { event ->
                    event is ServerWsEvent.NewMessage && event.chatId == chatId
                }
                .collect { refreshTrigger.tryEmit(Unit) }
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
            replyToMediaThumbnailUrl = replyMediaThumb,
            reactions = parseReactions(reactionsJson),
            isScheduled = scheduledAt != null,
            scheduledAt = scheduledAt
        )
    }

    // ── Typing indicators ───────────────────────────────────────────────

    private fun observeTypingIndicators() {
        viewModelScope.launch {
            typingStateHolder.getTypingUsersForChat(chatId)
                .distinctUntilChanged()
                .collect { userIds ->
                    val names = userIds.map { uid ->
                        userDao.getById(uid)?.displayName ?: uid.take(8)
                    }.toSet()
                    _uiState.update { it.copy(typingUsers = names) }
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

    fun sendLocationMessage(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val content = "$latitude,$longitude"
            when (val result = sendMessageUseCase(chatId, content, messageType = "location")) {
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

    // ── Reactions ─────────────────────────────────────────────────────────

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            val result = messageRepository.toggleReaction(chatId, messageId, emoji)
            if (result is AppResult.Error) {
                _uiState.update { it.copy(error = result.message) }
            }
        }
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

    // ── Media messages ────────────────────────────────────────────────

    fun sendMediaMessage(uri: Uri, messageType: String, mimeType: String?) {
        viewModelScope.launch {
            val uploadResult = when (messageType) {
                "image" -> mediaRepository.uploadImage(uri, currentUserId)
                "video" -> mediaRepository.uploadVideo(uri, currentUserId)
                else -> mediaRepository.uploadDocument(uri, currentUserId)
            }

            when (uploadResult) {
                is AppResult.Success -> {
                    val media = uploadResult.data
                    val result = sendMessageUseCase(
                        chatId = chatId,
                        content = media.originalFilename ?: "",
                        messageType = messageType,
                        mediaId = media.mediaId,
                        mediaUrl = media.storageUrl,
                        mediaThumbnailUrl = media.thumbnailUrl,
                        mediaMimeType = media.mimeType,
                        mediaSize = media.sizeBytes,
                        mediaDuration = media.durationMs
                    )
                    if (result is AppResult.Error) {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = uploadResult.message) }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Voice recording ──────────────────────────────────────────────

    fun startRecording() {
        if (!voiceRecorder.startRecording()) {
            _uiState.update { it.copy(error = "Unable to start recording. Check microphone permission.") }
        }
    }

    fun stopRecording() {
        val result = voiceRecorder.stopRecording() ?: return
        uploadAndSendVoiceNote(result.file, result.durationMs)
    }

    fun cancelRecording() {
        voiceRecorder.cancelRecording()
    }

    fun lockRecording() {
        voiceRecorder.lockRecording()
    }

    fun sendRecording() {
        val result = voiceRecorder.stopRecording() ?: return
        uploadAndSendVoiceNote(result.file, result.durationMs)
    }

    private fun uploadAndSendVoiceNote(file: java.io.File, durationMs: Long) {
        viewModelScope.launch {
            val uri = Uri.fromFile(file)
            when (val uploadResult = mediaRepository.uploadDocument(uri, currentUserId)) {
                is AppResult.Success -> {
                    val media = uploadResult.data
                    val result = sendMessageUseCase(
                        chatId = chatId,
                        content = "",
                        messageType = "audio",
                        mediaId = media.mediaId,
                        mediaUrl = media.storageUrl,
                        mediaMimeType = "audio/mp4",
                        mediaSize = media.sizeBytes,
                        mediaDuration = durationMs.toInt()
                    )
                    if (result is AppResult.Error) {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = uploadResult.message) }
                }
                is AppResult.Loading -> Unit
            }
            file.delete()
        }
    }

    // ── Read receipts ───────────────────────────────────────────────────

    fun markAsRead() {
        viewModelScope.launch {
            markMessagesReadUseCase(chatId)
        }
    }

    // ── Mute / Unmute ────────────────────────────────────────────────

    fun toggleMute() {
        val currentlyMuted = _uiState.value.isMuted
        viewModelScope.launch {
            val result = chatRepository.muteChat(chatId, !currentlyMuted)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isMuted = !currentlyMuted) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is AppResult.Loading -> Unit
            }
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

    // ── Disappearing messages ─────────────────────────────────────────

    fun setDisappearingMessages(timer: String) {
        viewModelScope.launch {
            val result = chatRepository.setDisappearingTimer(chatId, timer)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(disappearingTimer = timer) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Multi-select ─────────────────────────────────────────────────────

    fun enterSelectionMode(messageId: String) {
        _uiState.update {
            it.copy(isSelectionMode = true, selectedMessageIds = setOf(messageId))
        }
    }

    fun toggleMessageSelection(messageId: String) {
        _uiState.update { state ->
            val updated = if (messageId in state.selectedMessageIds) {
                state.selectedMessageIds - messageId
            } else {
                state.selectedMessageIds + messageId
            }
            if (updated.isEmpty()) {
                state.copy(isSelectionMode = false, selectedMessageIds = emptySet())
            } else {
                state.copy(selectedMessageIds = updated)
            }
        }
    }

    fun exitSelectionMode() {
        _uiState.update {
            it.copy(isSelectionMode = false, selectedMessageIds = emptySet())
        }
    }

    fun deleteSelectedMessages() {
        val ids = _uiState.value.selectedMessageIds.toList()
        viewModelScope.launch {
            ids.forEach { id -> messageRepository.softDelete(id, forEveryone = false) }
        }
        exitSelectionMode()
    }

    fun starSelectedMessages() {
        val ids = _uiState.value.selectedMessageIds.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                val entity = messageDao.getById(id)
                if (entity != null) {
                    messageRepository.starMessage(id, !entity.isStarred)
                }
            }
        }
        exitSelectionMode()
    }

    fun copySelectedMessages() {
        viewModelScope.launch {
            val contents = _uiState.value.selectedMessageIds.mapNotNull { id ->
                messageDao.getById(id)?.content
            }
            if (contents.isNotEmpty()) {
                copyToClipboard(contents.joinToString("\n"))
            }
        }
        exitSelectionMode()
    }

    // ── Scheduled messages ─────────────────────────────────────────────

    fun scheduleMessage(scheduledAtMillis: Long) {
        val text = _uiState.value.composerText.trim()
        if (text.isBlank()) return

        _uiState.update { it.copy(composerText = "") }
        _replyToMessage.value = null

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = MessageEntity(
                messageId = "scheduled_${java.util.UUID.randomUUID()}",
                clientMsgId = java.util.UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = currentUserId,
                messageType = "text",
                content = text,
                status = "scheduled",
                timestamp = scheduledAtMillis,
                createdAt = now,
                scheduledAt = scheduledAtMillis
            )
            messageDao.insert(entity)
        }
    }

    // ── Chat export ──────────────────────────────────────────────────────

    fun exportChat() {
        viewModelScope.launch {
            try {
                val messages = messageDao.getAllForChat(chatId)
                if (messages.isEmpty()) {
                    _uiState.update { it.copy(error = "No messages to export") }
                    return@launch
                }

                val senderNameCache = mutableMapOf<String, String>()
                val lines = messages.sortedBy { it.timestamp }.map { msg ->
                    val senderName = senderNameCache.getOrPut(msg.senderId) {
                        if (msg.senderId == currentUserId) {
                            "You"
                        } else {
                            userDao.getById(msg.senderId)?.displayName ?: msg.senderId.take(8)
                        }
                    }
                    val time = TimeUtils.formatExportTimestamp(msg.timestamp)
                    val content = when {
                        msg.isDeleted -> "<deleted>"
                        msg.messageType != "text" && msg.content.isNullOrBlank() -> "<${msg.messageType}>"
                        else -> msg.content ?: ""
                    }
                    "[$time] $senderName: $content"
                }

                val chatName = _uiState.value.chatName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val file = java.io.File(appContext.cacheDir, "export_${chatName}.txt")
                file.writeText(lines.joinToString("\n"))

                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(
                    Intent.createChooser(shareIntent, "Export chat").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    // ── Error handling ──────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun parseReactions(jsonStr: String?): List<Reaction> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val obj = kotlinx.serialization.json.Json.decodeFromString(
                kotlinx.serialization.json.JsonObject.serializer(), jsonStr
            )
            obj.map { (emoji, value) ->
                val userIds = kotlinx.serialization.json.Json.decodeFromString<List<String>>(value.toString())
                Reaction(
                    emoji = emoji,
                    userIds = userIds,
                    isFromMe = currentUserId in userIds
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
