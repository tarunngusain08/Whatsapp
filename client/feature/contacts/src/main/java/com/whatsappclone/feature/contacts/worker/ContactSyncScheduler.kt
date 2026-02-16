package com.whatsappclone.feature.contacts.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.whatsappclone.core.common.util.Constants
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    /**
     * Schedules periodic contact sync every [Constants.CONTACT_SYNC_INTERVAL_HOURS] hours.
     * Should be called once after login / on app startup when user is authenticated.
     */
    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<ContactSyncWorker>(
            repeatInterval = Constants.CONTACT_SYNC_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30L,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            ContactSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Cancels any scheduled contact sync work.
     * Call on logout.
     */
    fun cancel() {
        workManager.cancelUniqueWork(ContactSyncWorker.WORK_NAME)
    }
}
