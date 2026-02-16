package com.whatsappclone.feature.media.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.whatsappclone.core.common.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Compresses an image from [uri]:
     * 1. Efficiently samples the bitmap using inSampleSize to avoid OOM.
     * 2. Scales down to [Constants.MEDIA_IMAGE_MAX_WIDTH] maintaining aspect ratio.
     * 3. Corrects EXIF rotation.
     * 4. Encodes as JPEG at quality [Constants.MEDIA_IMAGE_QUALITY].
     *
     * @return compressed [File] in the app cache directory, or null on failure.
     */
    fun compress(uri: Uri): File? {
        return try {
            val maxWidth = Constants.MEDIA_IMAGE_MAX_WIDTH
            val quality = Constants.MEDIA_IMAGE_QUALITY

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            // Step 1: Decode bounds only to calculate sample size
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            inputStream.use { BitmapFactory.decodeStream(it, null, options) }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) return null

            // Step 2: Calculate inSampleSize for efficient memory usage
            options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, maxWidth)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // Step 3: Decode the sampled bitmap
            val sampledStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val sampledBitmap = sampledStream.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null

            // Step 4: Scale down if still larger than maxWidth
            val scaledBitmap = scaleBitmap(sampledBitmap, maxWidth)

            // Step 5: Correct EXIF orientation
            val rotatedBitmap = correctOrientation(uri, scaledBitmap)

            // Step 6: Write compressed JPEG to cache
            val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { fos ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                fos.flush()
            }

            // Clean up intermediate bitmaps
            if (sampledBitmap !== scaledBitmap) sampledBitmap.recycle()
            if (scaledBitmap !== rotatedBitmap) scaledBitmap.recycle()
            rotatedBitmap.recycle()

            outputFile
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxWidth: Int
    ): Int {
        val longerSide = max(width, height)
        var sampleSize = 1
        while (longerSide / (sampleSize * 2) >= maxWidth) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val longerSide = max(bitmap.width, bitmap.height)
        if (longerSide <= maxWidth) return bitmap

        val scale = maxWidth.toFloat() / longerSide.toFloat()
        val newWidth = (bitmap.width * scale).roundToInt()
        val newHeight = (bitmap.height * scale).roundToInt()

        return Bitmap.createScaledBitmap(
            bitmap,
            max(1, newWidth),
            max(1, newHeight),
            true
        )
    }

    private fun correctOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return bitmap
            val exif = inputStream.use { ExifInterface(it) }
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap
            }

            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
