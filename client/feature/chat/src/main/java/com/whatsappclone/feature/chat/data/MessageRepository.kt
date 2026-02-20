package com.whatsappclone.feature.chat.data

import androidx.paging.PagingData
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.network.model.dto.MessageDto
import kotlinx.coroutines.flow.Flow

interface MessageRepository {

    fun observeMessages(chatId: String): Flow<PagingData<MessageEntity>>

    /**
     * Saves the message locally (status = PENDING), then attempts to send via WebSocket.
     * If WebSocket is not connected the message stays pending for [PendingMessageWorker].
     */
    suspend fun saveAndSend(
        chatId: String,
        content: String,
        messageType: String = "text",
        replyToMessageId: String? = null,
        mediaId: String? = null,
        mediaUrl: String? = null,
        mediaThumbnailUrl: String? = null,
        mediaMimeType: String? = null,
        mediaSize: Long? = null,
        mediaDuration: Int? = null
    ): AppResult<Unit>

    /**
     * Inserts a message received from a remote source (WebSocket / push).
     */
    suspend fun insertFromRemote(messageDto: MessageDto)

    /**
     * Called when the server acknowledges a message we sent.
     * Replaces the local clientMsgId-based row with the server-assigned id.
     */
    suspend fun confirmSent(clientMsgId: String, serverId: String, timestamp: Long)

    suspend fun updateStatus(messageId: String, status: String)

    suspend fun softDelete(messageId: String, forEveryone: Boolean)

    suspend fun starMessage(messageId: String, starred: Boolean): AppResult<Unit>

    suspend fun markRead(chatId: String, upToMessageId: String): AppResult<Unit>

    suspend fun toggleReaction(chatId: String, messageId: String, emoji: String): AppResult<Unit>

    suspend fun getAllPending(): List<MessageEntity>

    /**
     * Fallback REST send for messages that couldn't be delivered via WebSocket.
     */
    suspend fun sendViaRest(message: MessageEntity): AppResult<Unit>
}
