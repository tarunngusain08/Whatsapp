package com.whatsappclone.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey
    val mediaId: String,
    val uploaderId: String,
    val fileType: String,  // "image"|"video"|"audio"|"document"
    val mimeType: String,
    val originalFilename: String? = null,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Int? = null,
    val storageUrl: String,
    val thumbnailUrl: String? = null,
    val localPath: String? = null,
    val localThumbnailPath: String? = null,
    val createdAt: Long
)
