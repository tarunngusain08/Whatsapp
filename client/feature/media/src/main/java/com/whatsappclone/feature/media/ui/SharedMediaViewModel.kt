package com.whatsappclone.feature.media.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.database.dao.MediaDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.entity.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MediaItem(
    val messageId: String,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val mimeType: String?,
    val fileName: String?,
    val fileSize: Long?,
    val timestamp: Long,
    val messageType: String
)

data class LinkItem(
    val messageId: String,
    val url: String,
    val content: String?,
    val timestamp: Long
)

data class MonthGroup<T>(
    val label: String,
    val items: List<T>
)

data class SharedMediaUiState(
    val mediaGroups: List<MonthGroup<MediaItem>> = emptyList(),
    val documentGroups: List<MonthGroup<MediaItem>> = emptyList(),
    val linkGroups: List<MonthGroup<LinkItem>> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: SharedMediaTab = SharedMediaTab.MEDIA,
    val error: String? = null
) {
    val hasMedia: Boolean get() = mediaGroups.any { it.items.isNotEmpty() }
    val hasDocs: Boolean get() = documentGroups.any { it.items.isNotEmpty() }
    val hasLinks: Boolean get() = linkGroups.any { it.items.isNotEmpty() }
}

enum class SharedMediaTab { MEDIA, DOCS, LINKS }

@HiltViewModel
class SharedMediaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageDao: MessageDao,
    private val mediaDao: MediaDao
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _uiState = MutableStateFlow(SharedMediaUiState())
    val uiState: StateFlow<SharedMediaUiState> = _uiState.asStateFlow()

    init {
        loadSharedMedia()
    }

    fun onTabSelected(tab: SharedMediaTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun loadSharedMedia() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val allMessages = messageDao.getAllForChat(chatId)
                    .filter { !it.isDeleted }

                val mediaMessages = allMessages.filter {
                    it.messageType in listOf("image", "video") && it.mediaUrl != null
                }
                val documentMessages = allMessages.filter {
                    it.messageType == "document" && it.mediaUrl != null
                }

                val mediaItems = mediaMessages.map { it.toMediaItem() }
                val docItems = documentMessages.map { it.toMediaItem() }
                val linkItems = extractLinks(allMessages)

                _uiState.update {
                    it.copy(
                        mediaGroups = groupByMonth(mediaItems),
                        documentGroups = groupByMonth(docItems),
                        linkGroups = groupByMonth(linkItems),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    private suspend fun MessageEntity.toMediaItem(): MediaItem {
        val media = mediaId?.let { mediaDao.getById(it) }
        return MediaItem(
            messageId = messageId,
            mediaUrl = mediaUrl,
            thumbnailUrl = mediaThumbnailUrl ?: media?.thumbnailUrl,
            mimeType = mediaMimeType ?: media?.mimeType,
            fileName = media?.originalFilename,
            fileSize = mediaSize ?: media?.sizeBytes,
            timestamp = timestamp,
            messageType = messageType
        )
    }

    private fun extractLinks(messages: List<MessageEntity>): List<LinkItem> {
        val urlPattern = Regex(
            """https?://[^\s<>"{}|\\^`\[\]]+""",
            RegexOption.IGNORE_CASE
        )

        return messages
            .filter { it.messageType == "text" && it.content != null }
            .flatMap { message ->
                urlPattern.findAll(message.content!!)
                    .map { match ->
                        LinkItem(
                            messageId = message.messageId,
                            url = match.value,
                            content = message.content,
                            timestamp = message.timestamp
                        )
                    }
                    .toList()
            }
            .sortedByDescending { it.timestamp }
    }

    private fun <T> groupByMonth(items: List<T>): List<MonthGroup<T>> where T : Any {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val now = Calendar.getInstance()

        return items
            .sortedByDescending { getTimestamp(it) }
            .groupBy { item ->
                val ts = getTimestamp(item)
                val cal = Calendar.getInstance().apply { timeInMillis = ts }

                when {
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) -> "This month"
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) - 1 -> "Last month"
                    else -> formatter.format(Date(ts))
                }
            }
            .map { (label, groupItems) ->
                MonthGroup(label = label, items = groupItems)
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getTimestamp(item: T): Long {
        return when (item) {
            is MediaItem -> item.timestamp
            is LinkItem -> item.timestamp
            else -> 0L
        }
    }
}
