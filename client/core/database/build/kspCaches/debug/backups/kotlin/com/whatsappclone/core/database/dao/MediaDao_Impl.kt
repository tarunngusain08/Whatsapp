package com.whatsappclone.core.database.dao

import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.CoroutinesRoom
import androidx.room.CoroutinesRoom.Companion.execute
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.whatsappclone.core.database.entity.MediaEntity
import java.lang.Class
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MediaDao_Impl(
  __db: RoomDatabase,
) : MediaDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfMediaEntity: EntityInsertionAdapter<MediaEntity>

  private val __preparedStmtOfUpdateLocalPath: SharedSQLiteStatement

  private val __preparedStmtOfClearStaleLocalPaths: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfMediaEntity = object : EntityInsertionAdapter<MediaEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `media` (`mediaId`,`uploaderId`,`fileType`,`mimeType`,`originalFilename`,`sizeBytes`,`width`,`height`,`durationMs`,`storageUrl`,`thumbnailUrl`,`localPath`,`localThumbnailPath`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MediaEntity) {
        statement.bindString(1, entity.mediaId)
        statement.bindString(2, entity.uploaderId)
        statement.bindString(3, entity.fileType)
        statement.bindString(4, entity.mimeType)
        val _tmpOriginalFilename: String? = entity.originalFilename
        if (_tmpOriginalFilename == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpOriginalFilename)
        }
        statement.bindLong(6, entity.sizeBytes)
        val _tmpWidth: Int? = entity.width
        if (_tmpWidth == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpWidth.toLong())
        }
        val _tmpHeight: Int? = entity.height
        if (_tmpHeight == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpHeight.toLong())
        }
        val _tmpDurationMs: Int? = entity.durationMs
        if (_tmpDurationMs == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpDurationMs.toLong())
        }
        statement.bindString(10, entity.storageUrl)
        val _tmpThumbnailUrl: String? = entity.thumbnailUrl
        if (_tmpThumbnailUrl == null) {
          statement.bindNull(11)
        } else {
          statement.bindString(11, _tmpThumbnailUrl)
        }
        val _tmpLocalPath: String? = entity.localPath
        if (_tmpLocalPath == null) {
          statement.bindNull(12)
        } else {
          statement.bindString(12, _tmpLocalPath)
        }
        val _tmpLocalThumbnailPath: String? = entity.localThumbnailPath
        if (_tmpLocalThumbnailPath == null) {
          statement.bindNull(13)
        } else {
          statement.bindString(13, _tmpLocalThumbnailPath)
        }
        statement.bindLong(14, entity.createdAt)
      }
    }
    this.__preparedStmtOfUpdateLocalPath = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = """
            |
            |        UPDATE media 
            |        SET localPath = ?, localThumbnailPath = ? 
            |        WHERE mediaId = ?
            |        
            """.trimMargin()
        return _query
      }
    }
    this.__preparedStmtOfClearStaleLocalPaths = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = """
            |
            |        UPDATE media 
            |        SET localPath = NULL, localThumbnailPath = NULL 
            |        WHERE createdAt < ? AND localPath IS NOT NULL
            |        
            """.trimMargin()
        return _query
      }
    }
  }

  public override suspend fun insert(media: MediaEntity): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __insertionAdapterOfMediaEntity.insert(media)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateLocalPath(
    mediaId: String,
    localPath: String?,
    localThumbnailPath: String?,
  ): Unit = CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdateLocalPath.acquire()
      var _argIndex: Int = 1
      if (localPath == null) {
        _stmt.bindNull(_argIndex)
      } else {
        _stmt.bindString(_argIndex, localPath)
      }
      _argIndex = 2
      if (localThumbnailPath == null) {
        _stmt.bindNull(_argIndex)
      } else {
        _stmt.bindString(_argIndex, localThumbnailPath)
      }
      _argIndex = 3
      _stmt.bindString(_argIndex, mediaId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfUpdateLocalPath.release(_stmt)
      }
    }
  })

  public override suspend fun clearStaleLocalPaths(olderThan: Long): Int =
      CoroutinesRoom.execute(__db, true, object : Callable<Int> {
    public override fun call(): Int {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfClearStaleLocalPaths.acquire()
      var _argIndex: Int = 1
      _stmt.bindLong(_argIndex, olderThan)
      try {
        __db.beginTransaction()
        try {
          val _result: Int = _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
          return _result
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfClearStaleLocalPaths.release(_stmt)
      }
    }
  })

  public override suspend fun getById(mediaId: String): MediaEntity? {
    val _sql: String = "SELECT * FROM media WHERE mediaId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, mediaId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<MediaEntity?> {
      public override fun call(): MediaEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfUploaderId: Int = getColumnIndexOrThrow(_cursor, "uploaderId")
          val _cursorIndexOfFileType: Int = getColumnIndexOrThrow(_cursor, "fileType")
          val _cursorIndexOfMimeType: Int = getColumnIndexOrThrow(_cursor, "mimeType")
          val _cursorIndexOfOriginalFilename: Int = getColumnIndexOrThrow(_cursor,
              "originalFilename")
          val _cursorIndexOfSizeBytes: Int = getColumnIndexOrThrow(_cursor, "sizeBytes")
          val _cursorIndexOfWidth: Int = getColumnIndexOrThrow(_cursor, "width")
          val _cursorIndexOfHeight: Int = getColumnIndexOrThrow(_cursor, "height")
          val _cursorIndexOfDurationMs: Int = getColumnIndexOrThrow(_cursor, "durationMs")
          val _cursorIndexOfStorageUrl: Int = getColumnIndexOrThrow(_cursor, "storageUrl")
          val _cursorIndexOfThumbnailUrl: Int = getColumnIndexOrThrow(_cursor, "thumbnailUrl")
          val _cursorIndexOfLocalPath: Int = getColumnIndexOrThrow(_cursor, "localPath")
          val _cursorIndexOfLocalThumbnailPath: Int = getColumnIndexOrThrow(_cursor,
              "localThumbnailPath")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _result: MediaEntity?
          if (_cursor.moveToFirst()) {
            val _tmpMediaId: String
            _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            val _tmpUploaderId: String
            _tmpUploaderId = _cursor.getString(_cursorIndexOfUploaderId)
            val _tmpFileType: String
            _tmpFileType = _cursor.getString(_cursorIndexOfFileType)
            val _tmpMimeType: String
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType)
            val _tmpOriginalFilename: String?
            if (_cursor.isNull(_cursorIndexOfOriginalFilename)) {
              _tmpOriginalFilename = null
            } else {
              _tmpOriginalFilename = _cursor.getString(_cursorIndexOfOriginalFilename)
            }
            val _tmpSizeBytes: Long
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes)
            val _tmpWidth: Int?
            if (_cursor.isNull(_cursorIndexOfWidth)) {
              _tmpWidth = null
            } else {
              _tmpWidth = _cursor.getInt(_cursorIndexOfWidth)
            }
            val _tmpHeight: Int?
            if (_cursor.isNull(_cursorIndexOfHeight)) {
              _tmpHeight = null
            } else {
              _tmpHeight = _cursor.getInt(_cursorIndexOfHeight)
            }
            val _tmpDurationMs: Int?
            if (_cursor.isNull(_cursorIndexOfDurationMs)) {
              _tmpDurationMs = null
            } else {
              _tmpDurationMs = _cursor.getInt(_cursorIndexOfDurationMs)
            }
            val _tmpStorageUrl: String
            _tmpStorageUrl = _cursor.getString(_cursorIndexOfStorageUrl)
            val _tmpThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl)
            }
            val _tmpLocalPath: String?
            if (_cursor.isNull(_cursorIndexOfLocalPath)) {
              _tmpLocalPath = null
            } else {
              _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath)
            }
            val _tmpLocalThumbnailPath: String?
            if (_cursor.isNull(_cursorIndexOfLocalThumbnailPath)) {
              _tmpLocalThumbnailPath = null
            } else {
              _tmpLocalThumbnailPath = _cursor.getString(_cursorIndexOfLocalThumbnailPath)
            }
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            _result =
                MediaEntity(_tmpMediaId,_tmpUploaderId,_tmpFileType,_tmpMimeType,_tmpOriginalFilename,_tmpSizeBytes,_tmpWidth,_tmpHeight,_tmpDurationMs,_tmpStorageUrl,_tmpThumbnailUrl,_tmpLocalPath,_tmpLocalThumbnailPath,_tmpCreatedAt)
          } else {
            _result = null
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
