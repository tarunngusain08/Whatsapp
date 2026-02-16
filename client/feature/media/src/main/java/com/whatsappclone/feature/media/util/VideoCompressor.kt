package com.whatsappclone.feature.media.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder video compressor.
 *
 * In a production app this would use MediaCodec / MediaMuxer (or a library like
 * LightCompressor) to re-encode at a target bitrate. For now it simply copies the
 * file bytes to a cache-dir location so the rest of the pipeline has a local [File].
 */
@Singleton
class VideoCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Copies the video at [uri] into the cache directory and returns the local [File].
     *
     * @return the copied [File], or null if the operation fails.
     */
    fun compress(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            val extension = context.contentResolver.getType(uri)
                ?.substringAfter("/")
                ?.takeIf { it.isNotBlank() }
                ?: "mp4"

            val outputFile = File(
                context.cacheDir,
                "video_${System.currentTimeMillis()}.$extension"
            )

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                    output.flush()
                }
            }

            outputFile
        } catch (e: Exception) {
            null
        }
    }
}
