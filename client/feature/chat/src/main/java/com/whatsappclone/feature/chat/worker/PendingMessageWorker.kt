package com.whatsappclone.feature.chat.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.feature.chat.data.MessageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PendingMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageRepository: MessageRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendingMessages = messageRepository.getAllPending()
        if (pendingMessages.isEmpty()) return Result.success()

        var allSucceeded = true
        for (message in pendingMessages) {
            val result = messageRepository.sendViaRest(message)
            if (result is AppResult.Error) {
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success()
        else if (runAttemptCount < MAX_RETRIES) Result.retry()
        else Result.failure()
    }

    companion object {
        const val MAX_RETRIES = 5
        const val WORK_NAME = "pending_messages_flush"
    }
}
