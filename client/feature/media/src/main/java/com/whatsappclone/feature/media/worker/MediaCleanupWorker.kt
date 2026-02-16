package com.whatsappclone.feature.media.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.database.dao.MediaDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class MediaCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaDao: MediaDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val cutoff = System.currentTimeMillis() -
                TimeUnit.DAYS.toMillis(Constants.MEDIA_CACHE_MAX_AGE_DAYS.toLong())

            // Reset local paths in DB for stale entries
            val dbCleared = mediaDao.clearStaleLocalPaths(cutoff)

            // Walk the app-private media directory and delete old files
            val mediaDir = File(applicationContext.filesDir, "media")
            var filesDeleted = 0
            var bytesFreed = 0L

            if (mediaDir.exists() && mediaDir.isDirectory) {
                mediaDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < cutoff) {
                        val size = file.length()
                        if (file.delete()) {
                            filesDeleted++
                            bytesFreed += size
                        }
                    }
                }
            }

            Log.i(
                TAG,
                "Media cleanup complete: " +
                    "dbPathsCleared=$dbCleared, " +
                    "filesDeleted=$filesDeleted, " +
                    "bytesFreed=${formatBytes(bytesFreed)}"
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Media cleanup failed", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }

    companion object {
        const val TAG = "MediaCleanupWorker"
        const val WORK_NAME = "media_cleanup_periodic"
        const val MAX_RETRIES = 3
    }
}
