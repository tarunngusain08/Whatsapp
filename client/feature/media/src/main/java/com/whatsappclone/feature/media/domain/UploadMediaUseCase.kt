package com.whatsappclone.feature.media.domain

import android.content.Context
import android.net.Uri
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.database.entity.MediaEntity
import com.whatsappclone.feature.media.data.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Determines the media type from the URI's MIME type and delegates to the
 * appropriate [MediaRepository] upload method.
 */
class UploadMediaUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {

    suspend operator fun invoke(
        uri: Uri,
        uploaderId: String
    ): AppResult<MediaEntity> {
        val mimeType = context.contentResolver.getType(uri)
            ?: return AppResult.Error(
                ErrorCode.VALIDATION_ERROR,
                "Unable to determine file type"
            )

        return when {
            mimeType.startsWith("image/") -> {
                mediaRepository.uploadImage(uri, uploaderId)
            }

            mimeType.startsWith("video/") -> {
                mediaRepository.uploadVideo(uri, uploaderId)
            }

            mimeType.startsWith("audio/") -> {
                // Audio is uploaded as a document for now
                mediaRepository.uploadDocument(uri, uploaderId)
            }

            else -> {
                mediaRepository.uploadDocument(uri, uploaderId)
            }
        }
    }
}
