package com.whatsappclone.feature.chat.domain

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.feature.chat.data.ChatRepository
import javax.inject.Inject

class CreateDirectChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Creates a 1-to-1 direct chat with [otherUserId].
     * If a direct chat already exists, the existing chatId is returned.
     *
     * @return [AppResult.Success] containing the chatId on success.
     */
    suspend operator fun invoke(otherUserId: String): AppResult<String> {
        if (otherUserId.isBlank()) {
            return AppResult.Error(
                ErrorCode.VALIDATION_ERROR,
                "User ID cannot be empty"
            )
        }
        return chatRepository.createDirectChat(otherUserId)
    }
}
