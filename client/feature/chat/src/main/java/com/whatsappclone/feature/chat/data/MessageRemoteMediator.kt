package com.whatsappclone.feature.chat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.network.api.MessageApi
import com.whatsappclone.core.network.model.dto.MessageDto
import java.time.Instant

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val chatId: String,
    private val messageApi: MessageApi,
    private val messageDao: MessageDao
) : RemoteMediator<Int, MessageEntity>() {

    private var nextCursor: String? = null

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>
    ): MediatorResult {
        return try {
            val cursor = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> {
                    // We only paginate backwards (older messages), so PREPEND is always done
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    nextCursor ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val response = messageApi.getMessages(
                chatId = chatId,
                cursor = cursor,
                limit = PAGE_SIZE
            )

            if (!response.isSuccessful) {
                return MediatorResult.Error(
                    Exception("API error: HTTP ${response.code()} ${response.message()}")
                )
            }

            val body = response.body()
            if (body == null || !body.success || body.data == null) {
                return MediatorResult.Error(
                    Exception(body?.error?.message ?: "Unexpected response format")
                )
            }

            val paginatedData = body.data!!
            val entities = paginatedData.items.map { it.toEntity() }

            messageDao.insertAll(entities)
            nextCursor = paginatedData.nextCursor

            MediatorResult.Success(endOfPaginationReached = !paginatedData.hasMore)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private fun MessageDto.toEntity(): MessageEntity {
        val epochMillis = try {
            Instant.parse(createdAt).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        return MessageEntity(
            messageId = messageId,
            clientMsgId = clientMsgId ?: messageId,
            chatId = chatId,
            senderId = senderId,
            messageType = type,
            content = payload.body,
            mediaId = payload.mediaId,
            mediaUrl = payload.mediaUrl,
            mediaThumbnailUrl = payload.thumbnailUrl,
            mediaMimeType = payload.mimeType,
            mediaSize = payload.fileSize,
            mediaDuration = payload.duration,
            replyToMessageId = replyToMessageId,
            status = status,
            isDeleted = isDeleted,
            isStarred = isStarred,
            timestamp = epochMillis,
            createdAt = epochMillis
        )
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
