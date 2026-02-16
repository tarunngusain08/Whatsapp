package com.whatsappclone.feature.media.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.database.dao.MediaDao
import com.whatsappclone.core.database.entity.MediaEntity
import com.whatsappclone.core.network.api.MediaApi
import com.whatsappclone.core.network.model.safeApiCall
import com.whatsappclone.feature.media.util.ImageCompressor
import com.whatsappclone.feature.media.util.VideoCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaApi: MediaApi,
    private val mediaDao: MediaDao,
    private val imageCompressor: ImageCompressor,
    private val videoCompressor: VideoCompressor
) : MediaRepository {

    private val mediaDir: File by lazy {
        File(context.filesDir, "media").also { it.mkdirs() }
    }

    // ── Upload Image ─────────────────────────────────────────────────────

    override suspend fun uploadImage(uri: Uri, uploaderId: String): AppResult<MediaEntity> {
        val compressed = imageCompressor.compress(uri)
            ?: return AppResult.Error(
                ErrorCode.UNKNOWN,
                "Failed to compress image"
            )

        return uploadFile(
            file = compressed,
            fileType = "image",
            mimeType = "image/jpeg",
            originalFilename = queryFileName(uri),
            uploaderId = uploaderId
        ).also { compressed.delete() }
    }

    // ── Upload Video ─────────────────────────────────────────────────────

    override suspend fun uploadVideo(uri: Uri, uploaderId: String): AppResult<MediaEntity> {
        val copied = videoCompressor.compress(uri)
            ?: return AppResult.Error(
                ErrorCode.UNKNOWN,
                "Failed to prepare video file"
            )

        val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"

        return uploadFile(
            file = copied,
            fileType = "video",
            mimeType = mimeType,
            originalFilename = queryFileName(uri),
            uploaderId = uploaderId
        ).also { copied.delete() }
    }

    // ── Upload Document ──────────────────────────────────────────────────

    override suspend fun uploadDocument(uri: Uri, uploaderId: String): AppResult<MediaEntity> {
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val filename = queryFileName(uri) ?: "document"
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType) ?: "bin"

        val tempFile = File(context.cacheDir, "doc_${System.currentTimeMillis()}.$extension")

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return AppResult.Error(ErrorCode.UNKNOWN, "Cannot open document")

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                    output.flush()
                }
            }
        } catch (e: Exception) {
            tempFile.delete()
            return AppResult.Error(
                ErrorCode.UNKNOWN,
                e.message ?: "Failed to read document",
                e
            )
        }

        return uploadFile(
            file = tempFile,
            fileType = "document",
            mimeType = mimeType,
            originalFilename = filename,
            uploaderId = uploaderId
        ).also { tempFile.delete() }
    }

    // ── Query / Cache ────────────────────────────────────────────────────

    override suspend fun getMedia(mediaId: String): MediaEntity? {
        return mediaDao.getById(mediaId)
    }

    override suspend fun downloadMedia(mediaId: String, url: String): AppResult<File> {
        // Check if we already have a valid local file
        val existing = mediaDao.getById(mediaId)
        if (existing?.localPath != null) {
            val localFile = File(existing.localPath)
            if (localFile.exists()) return AppResult.Success(localFile)
        }

        return try {
            val extension = url.substringAfterLast('.', "bin")
                .substringBefore('?')
                .takeIf { it.length in 1..5 }
                ?: "bin"

            val destFile = File(mediaDir, "${mediaId}.$extension")

            // Download using OkHttp (via URL stream)
            val connection = java.net.URL(url).openConnection()
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.getInputStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                    output.flush()
                }
            }

            // Update Room with the local path
            mediaDao.updateLocalPath(mediaId, destFile.absolutePath, null)

            AppResult.Success(destFile)
        } catch (e: Exception) {
            AppResult.Error(
                ErrorCode.NETWORK_ERROR,
                e.message ?: "Failed to download media",
                e
            )
        }
    }

    override suspend fun clearStaleCache() {
        val cutoff = System.currentTimeMillis() -
            TimeUnit.DAYS.toMillis(Constants.MEDIA_CACHE_MAX_AGE_DAYS.toLong())

        // Clear DB references and get count of stale entries
        mediaDao.clearStaleLocalPaths(cutoff)

        // Walk the media directory and delete old files
        mediaDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private suspend fun uploadFile(
        file: File,
        fileType: String,
        mimeType: String,
        originalFilename: String?,
        uploaderId: String
    ): AppResult<MediaEntity> {
        val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val multipartFile = MultipartBody.Part.createFormData(
            "file",
            originalFilename ?: file.name,
            requestBody
        )
        val typePart = fileType.toRequestBody("text/plain".toMediaTypeOrNull())

        val result = safeApiCall { mediaApi.uploadMedia(multipartFile, typePart) }

        return when (result) {
            is AppResult.Success -> {
                val response = result.data
                val entity = MediaEntity(
                    mediaId = response.mediaId,
                    uploaderId = uploaderId,
                    fileType = response.type,
                    mimeType = response.mimeType,
                    originalFilename = originalFilename,
                    sizeBytes = response.sizeBytes,
                    width = response.width,
                    height = response.height,
                    durationMs = response.durationMs,
                    storageUrl = response.url,
                    thumbnailUrl = response.thumbnailUrl,
                    localPath = file.absolutePath,
                    createdAt = System.currentTimeMillis()
                )
                mediaDao.insert(entity)
                AppResult.Success(entity)
            }
            is AppResult.Error -> result
            is AppResult.Loading -> AppResult.Error(ErrorCode.UNKNOWN, "Unexpected loading state")
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
