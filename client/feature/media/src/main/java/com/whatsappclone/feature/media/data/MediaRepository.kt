package com.whatsappclone.feature.media.data

import android.net.Uri
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.entity.MediaEntity
import java.io.File

interface MediaRepository {

    /**
     * Compresses the image, uploads it via the media API, and persists the
     * resulting [MediaEntity] to Room.
     */
    suspend fun uploadImage(uri: Uri, uploaderId: String): AppResult<MediaEntity>

    /**
     * Copies (placeholder-compresses) the video, uploads it, and persists the entity.
     */
    suspend fun uploadVideo(uri: Uri, uploaderId: String): AppResult<MediaEntity>

    /**
     * Reads raw bytes from the URI, uploads as a document, and persists the entity.
     */
    suspend fun uploadDocument(uri: Uri, uploaderId: String): AppResult<MediaEntity>

    /**
     * Returns cached [MediaEntity] from Room by its id, or null if not found.
     */
    suspend fun getMedia(mediaId: String): MediaEntity?

    /**
     * Downloads the media file from [url] if not already cached locally.
     * On success the local path is persisted in the [MediaEntity].
     *
     * @return the downloaded [File].
     */
    suspend fun downloadMedia(mediaId: String, url: String): AppResult<File>

    /**
     * Deletes locally-cached media files older than [Constants.MEDIA_CACHE_MAX_AGE_DAYS]
     * and clears the corresponding local paths in Room.
     */
    suspend fun clearStaleCache()
}
