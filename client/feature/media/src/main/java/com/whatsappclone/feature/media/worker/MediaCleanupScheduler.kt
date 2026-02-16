package com.whatsappclone.feature.media.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaCleanupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Enqueues a periodic WorkManager job that runs [MediaCleanupWorker]
     * every 24 hours. Uses [ExistingPeriodicWorkPolicy.KEEP] so that
     * re-scheduling after a cold start doesn't reset the timer.
     */
    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<MediaCleanupWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(INITIAL_DELAY_HOURS, TimeUnit.HOURS)
            .addTag(MediaCleanupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MediaCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Cancels the periodic cleanup work.
     */
    fun cancel() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(MediaCleanupWorker.WORK_NAME)
    }

    companion object {
        private const val REPEAT_INTERVAL_HOURS = 24L
        private const val INITIAL_DELAY_HOURS = 1L
    }
}
