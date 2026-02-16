package com.whatsappclone.feature.contacts.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.feature.contacts.data.ContactRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ContactSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val contactRepository: ContactRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "contact_sync_worker"
    }

    override suspend fun doWork(): Result {
        return when (contactRepository.syncContacts()) {
            is AppResult.Success -> Result.success()
            is AppResult.Error -> {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
            is AppResult.Loading -> Result.retry()
        }
    }
}
