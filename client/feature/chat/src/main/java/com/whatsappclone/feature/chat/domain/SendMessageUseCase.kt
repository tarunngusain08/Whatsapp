package com.whatsappclone.feature.chat.domain

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.feature.chat.data.MessageRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        chatId: String,
        content: String,
        messageType: String = "text",
        replyToMessageId: String? = null,
        mediaId: String? = null,
        mediaUrl: String? = null,
        mediaThumbnailUrl: String? = null,
        mediaMimeType: String? = null,
        mediaSize: Long? = null,
        mediaDuration: Long? = null
    ): AppResult<Unit> {
        if (content.isBlank() && mediaId == null) {
            return AppResult.Error(
                ErrorCode.VALIDATION_ERROR,
                "Message cannot be empty"
            )
        }
        return messageRepository.saveAndSend(
            chatId = chatId,
            content = content,
            messageType = messageType,
            replyToMessageId = replyToMessageId,
            mediaId = mediaId,
            mediaUrl = mediaUrl,
            mediaThumbnailUrl = mediaThumbnailUrl,
            mediaMimeType = mediaMimeType,
            mediaSize = mediaSize,
            mediaDuration = mediaDuration
        )
    }
}
