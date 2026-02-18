package com.whatsappclone.feature.chat.domain

import com.whatsappclone.core.database.relation.ChatWithLastMessage
import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.model.ChatItemUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatItemUi>> {
        return chatRepository.observeChats().map { chats ->
            chats.map { it.toUiModel() }
        }
    }

    private fun ChatWithLastMessage.toUiModel(): ChatItemUi {
        val resolvedName = if (chat.chatType == "direct") {
            directChatOtherUserName ?: chat.name ?: "Unknown"
        } else {
            chat.name ?: "Group"
        }
        val resolvedAvatar = if (chat.chatType == "direct") {
            directChatOtherUserAvatarUrl ?: chat.avatarUrl
        } else {
            chat.avatarUrl
        }
        return ChatItemUi(
            chatId = chat.chatId,
            name = resolvedName,
            avatarUrl = resolvedAvatar,
            lastMessagePreview = formatPreview(lastMessageType, lastMessageText),
            lastMessageTimestamp = chat.lastMessageTimestamp,
            lastMessageSenderName = lastMessageSenderName,
            unreadCount = chat.unreadCount,
            isMuted = chat.isMuted,
            isPinned = chat.isPinned,
            chatType = chat.chatType,
            formattedTime = formatTimestamp(chat.lastMessageTimestamp)
        )
    }

    private fun formatPreview(messageType: String?, text: String?): String? {
        return when (messageType) {
            "image" -> "\uD83D\uDCF7 Photo"
            "video" -> "\uD83C\uDFA5 Video"
            "audio" -> "\uD83C\uDFA4 Audio"
            "document" -> "\uD83D\uDCC4 Document"
            "text" -> text
            else -> text
        }
    }

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return ""

        val messageDate = Date(timestamp)
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = messageDate }

        return when {
            isSameDay(now, msgCal) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
            }
            isYesterday(now, msgCal) -> "Yesterday"
            isSameWeek(now, msgCal) -> {
                SimpleDateFormat("EEE", Locale.getDefault()).format(messageDate)
            }
            else -> {
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate)
            }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(now: Calendar, other: Calendar): Boolean {
        val yesterday = now.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, other)
    }

    private fun isSameWeek(now: Calendar, other: Calendar): Boolean =
        now.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == other.get(Calendar.WEEK_OF_YEAR)
}
