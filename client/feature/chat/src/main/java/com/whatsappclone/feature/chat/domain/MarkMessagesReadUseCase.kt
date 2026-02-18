package com.whatsappclone.feature.chat.domain

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.network.websocket.WebSocketManager
import com.whatsappclone.core.network.websocket.WsConnectionState
import com.whatsappclone.core.network.websocket.WsFrame
import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.data.MessageRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class MarkMessagesReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val messageDao: MessageDao,
    private val webSocketManager: WebSocketManager
) {
    suspend operator fun invoke(chatId: String): AppResult<Unit> {
        val recentMessages = messageDao.observeRecentMessages(chatId, limit = 1)
            .first()
        val latest = recentMessages.firstOrNull()
            ?: return AppResult.Error(ErrorCode.NOT_FOUND, "No messages found in chat")

        val upToMessageId = latest.messageId

        if (webSocketManager.connectionState.value == WsConnectionState.CONNECTED) {
            val data = buildJsonObject {
                put("message_id", JsonPrimitive(upToMessageId))
            }
            webSocketManager.send(WsFrame(event = "message.read", data = data))
        }

        val markResult = messageRepository.markRead(chatId, upToMessageId)
        if (markResult is AppResult.Error) return markResult

        chatRepository.resetUnreadCount(chatId)
        return AppResult.Success(Unit)
    }
}
