package com.whatsappclone.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.whatsappclone.core.database.entity.MediaEntity

@Dao
interface MediaDao {

    @Query("SELECT * FROM media WHERE mediaId = :mediaId")
    suspend fun getById(mediaId: String): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Query(
        """
        UPDATE media 
        SET localPath = :localPath, localThumbnailPath = :localThumbnailPath 
        WHERE mediaId = :mediaId
        """
    )
    suspend fun updateLocalPath(mediaId: String, localPath: String?, localThumbnailPath: String?)

    @Query(
        """
        UPDATE media 
        SET localPath = NULL, localThumbnailPath = NULL 
        WHERE createdAt < :olderThan AND localPath IS NOT NULL
        """
    )
    suspend fun clearStaleLocalPaths(olderThan: Long): Int
}
