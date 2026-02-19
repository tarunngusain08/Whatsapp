package com.whatsappclone.feature.chat.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.feature.chat.data.MessageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduledMessageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val messageDao: MessageDao,
    private val messageRepository: MessageRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val dueMessages = messageDao.getDueScheduledMessages(now)

        for (message in dueMessages) {
            try {
                messageRepository.saveAndSend(
                    chatId = message.chatId,
                    content = message.content ?: "",
                    messageType = message.messageType,
                    replyToMessageId = message.replyToMessageId,
                    mediaId = message.mediaId,
                    mediaUrl = message.mediaUrl,
                    mediaMimeType = message.mediaMimeType,
                    mediaSize = message.mediaSize,
                    mediaDuration = message.mediaDuration?.toLong()
                )
                messageDao.softDelete(message.messageId, forEveryone = false)
            } catch (_: Exception) {
                // Will retry on next periodic run
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "scheduled_message_worker"
    }
}
