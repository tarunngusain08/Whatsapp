package com.whatsappclone.feature.media.util

import android.content.Context
import com.whatsappclone.core.database.dao.MediaDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the download state of a single media item.
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float = 0f) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

/**
 * Singleton download manager that:
 * - Checks the local cache before starting a download.
 * - Tracks in-progress downloads to avoid duplicate requests.
 * - Persists local paths in [MediaDao] on completion.
 * - Exposes per-media download state as [StateFlow] for UI observation.
 */
@Singleton
class MediaDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mediaDir: File by lazy {
        File(context.filesDir, "media").also { it.mkdirs() }
    }

    /** Active download states keyed by mediaId. */
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())

    /** Guards in-progress download tracking to avoid race conditions. */
    private val mutex = Mutex()

    /** Set of currently downloading media IDs. */
    private val inProgress = mutableSetOf<String>()

    /**
     * Returns the current download state for [mediaId].
     * UI composables can collect this flow to show progress/completion.
     */
    fun observeDownloadState(mediaId: String): StateFlow<Map<String, DownloadState>> =
        _downloadStates.asStateFlow()

    /**
     * Returns the current [DownloadState] for a given [mediaId], or [DownloadState.Idle].
     */
    fun getState(mediaId: String): DownloadState {
        return _downloadStates.value[mediaId] ?: DownloadState.Idle
    }

    /**
     * Enqueues a download for [mediaId] from [url].
     *
     * If the file already exists locally it returns immediately with [DownloadState.Completed].
     * If a download is already in progress for this id, it is a no-op.
     */
    fun enqueueDownload(mediaId: String, url: String) {
        scope.launch {
            // Check cache first
            val entity = mediaDao.getById(mediaId)
            if (entity?.localPath != null) {
                val localFile = File(entity.localPath)
                if (localFile.exists()) {
                    updateState(mediaId, DownloadState.Completed(localFile))
                    return@launch
                }
            }

            // Guard against duplicate downloads
            val shouldStart = mutex.withLock {
                if (mediaId in inProgress) false
                else {
                    inProgress.add(mediaId)
                    true
                }
            }
            if (!shouldStart) return@launch

            updateState(mediaId, DownloadState.Downloading(0f))

            try {
                val extension = url.substringAfterLast('.', "bin")
                    .substringBefore('?')
                    .takeIf { it.length in 1..5 }
                    ?: "bin"

                val destFile = File(mediaDir, "${mediaId}.$extension")

                val connection = java.net.URL(url).openConnection().apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                }

                val contentLength = connection.contentLengthLong
                var totalRead = 0L

                connection.getInputStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                                updateState(mediaId, DownloadState.Downloading(progress))
                            }
                        }
                        output.flush()
                    }
                }

                // Persist local path
                mediaDao.updateLocalPath(mediaId, destFile.absolutePath, null)
                updateState(mediaId, DownloadState.Completed(destFile))
            } catch (e: Exception) {
                updateState(mediaId, DownloadState.Failed(e.message ?: "Download failed"))
            } finally {
                mutex.withLock { inProgress.remove(mediaId) }
            }
        }
    }

    private fun updateState(mediaId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(mediaId, state)
        }
    }
}
