package com.whatsappclone.feature.group.domain

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.feature.chat.data.ChatRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    suspend operator fun invoke(
        name: String,
        participantIds: List<String>
    ): AppResult<String> {
        val trimmedName = name.trim()

        if (trimmedName.isBlank()) {
            return AppResult.Error(
                code = ErrorCode.VALIDATION_ERROR,
                message = "Group name cannot be empty"
            )
        }

        if (trimmedName.length > Constants.MAX_GROUP_NAME_LENGTH) {
            return AppResult.Error(
                code = ErrorCode.VALIDATION_ERROR,
                message = "Group name cannot exceed ${Constants.MAX_GROUP_NAME_LENGTH} characters"
            )
        }

        if (participantIds.isEmpty()) {
            return AppResult.Error(
                code = ErrorCode.VALIDATION_ERROR,
                message = "At least 1 participant is required"
            )
        }

        if (participantIds.size > Constants.MAX_GROUP_MEMBERS) {
            return AppResult.Error(
                code = ErrorCode.VALIDATION_ERROR,
                message = "Groups cannot have more than ${Constants.MAX_GROUP_MEMBERS} members"
            )
        }

        return chatRepository.createGroupChat(
            name = trimmedName,
            participantIds = participantIds
        )
    }
}
