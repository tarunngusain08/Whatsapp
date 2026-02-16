package com.whatsappclone.feature.chat.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ContactDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.database.relation.ContactWithUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val contactResults: List<ContactSearchResult> = emptyList(),
    val messageResults: List<MessageSearchResult> = emptyList(),
    val hasResults: Boolean = false,
    val showEmptyState: Boolean = false
)

data class ContactSearchResult(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val statusText: String?,
    val chatId: String? = null
)

data class MessageSearchResult(
    val messageId: String,
    val chatId: String,
    val chatName: String,
    val chatAvatarUrl: String?,
    val content: String,
    val senderId: String,
    val timestamp: Long,
    val formattedTime: String,
    val matchHighlightRange: IntRange? = null
)

sealed class GlobalSearchEvent {
    data class NavigateToChat(val chatId: String) : GlobalSearchEvent()
    data class NavigateToMessage(val chatId: String, val messageId: String) : GlobalSearchEvent()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GlobalSearchEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GlobalSearchEvent> = _events.asSharedFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        observeQueryChanges()
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun onContactClicked(result: ContactSearchResult) {
        val chatId = result.chatId
        if (chatId != null) {
            _events.tryEmit(GlobalSearchEvent.NavigateToChat(chatId))
        }
    }

    fun onMessageClicked(result: MessageSearchResult) {
        _events.tryEmit(
            GlobalSearchEvent.NavigateToMessage(
                chatId = result.chatId,
                messageId = result.messageId
            )
        )
    }

    fun clearSearch() {
        _uiState.value = GlobalSearchUiState()
        queryFlow.value = ""
    }

    private fun observeQueryChanges() {
        viewModelScope.launch {
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank() || query.length < MIN_QUERY_LENGTH) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                contactResults = emptyList(),
                                messageResults = emptyList(),
                                hasResults = false,
                                showEmptyState = false
                            )
                        }
                        return@collect
                    }

                    performSearch(query.trim())
                }
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            try {
                // Search contacts
                val contactResults = searchContacts(query)

                // Search messages via FTS
                val ftsQuery = sanitizeFtsQuery(query)
                val messageEntities = if (ftsQuery.isNotBlank()) {
                    messageDao.searchMessages(ftsQuery, MAX_MESSAGE_RESULTS)
                } else {
                    emptyList()
                }

                // Resolve chat names for messages
                val messageResults = resolveMessageResults(messageEntities, query)

                _uiState.update {
                    it.copy(
                        isSearching = false,
                        contactResults = contactResults,
                        messageResults = messageResults,
                        hasResults = contactResults.isNotEmpty() || messageResults.isNotEmpty(),
                        showEmptyState = contactResults.isEmpty() && messageResults.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, showEmptyState = true)
                }
            }
        }
    }

    private suspend fun searchContacts(query: String): List<ContactSearchResult> {
        val results = mutableListOf<ContactSearchResult>()

        // Collect first emission from Flow
        var contacts: List<ContactWithUser> = emptyList()
        val job = viewModelScope.launch {
            contactDao.searchRegisteredContacts(query).collect {
                contacts = it
                return@collect
            }
        }
        job.join()

        contacts.take(MAX_CONTACT_RESULTS).forEach { contact ->
            val userId = contact.userId ?: return@forEach
            val chatId = chatDao.findDirectChatWithUser(
                currentUserId = "", // Will be resolved at the screen level
                otherUserId = userId
            )

            results.add(
                ContactSearchResult(
                    userId = userId,
                    displayName = contact.displayName ?: contact.deviceName,
                    avatarUrl = contact.avatarUrl,
                    statusText = contact.statusText,
                    chatId = chatId
                )
            )
        }

        return results
    }

    private suspend fun resolveMessageResults(
        entities: List<MessageEntity>,
        query: String
    ): List<MessageSearchResult> {
        val chatCache = mutableMapOf<String, ChatEntity?>()

        return entities.mapNotNull { msg ->
            val content = msg.content ?: return@mapNotNull null

            val chat = chatCache.getOrPut(msg.chatId) {
                chatDao.getChatById(msg.chatId)
            } ?: return@mapNotNull null

            val matchIndex = content.lowercase().indexOf(query.lowercase())
            val highlightRange = if (matchIndex >= 0) {
                matchIndex until (matchIndex + query.length)
            } else null

            MessageSearchResult(
                messageId = msg.messageId,
                chatId = msg.chatId,
                chatName = chat.name ?: "Chat",
                chatAvatarUrl = chat.avatarUrl,
                content = content,
                senderId = msg.senderId,
                timestamp = msg.timestamp,
                formattedTime = formatSearchTime(msg.timestamp),
                matchHighlightRange = highlightRange
            )
        }
    }

    private fun sanitizeFtsQuery(query: String): String {
        // Escape special FTS characters and append wildcard for prefix matching
        val sanitized = query
            .replace("\"", "")
            .replace("*", "")
            .replace("(", "")
            .replace(")", "")
            .trim()

        return if (sanitized.isNotBlank()) "\"$sanitized*\"" else ""
    }

    private fun formatSearchTime(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            days < 1 -> {
                val hours = diff / (60 * 60 * 1000)
                val minutes = diff / (60 * 1000)
                when {
                    hours > 0 -> "${hours}h ago"
                    minutes > 0 -> "${minutes}m ago"
                    else -> "now"
                }
            }
            days < 7 -> "${days}d ago"
            else -> {
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = timestampMs
                }
                "${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.YEAR)}"
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val MIN_QUERY_LENGTH = 2
        private const val MAX_CONTACT_RESULTS = 10
        private const val MAX_MESSAGE_RESULTS = 30
    }
}
