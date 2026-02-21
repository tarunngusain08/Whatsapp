package com.whatsappclone.core.database.dao

import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.PagingSource
import androidx.room.CoroutinesRoom
import androidx.room.CoroutinesRoom.Companion.execute
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
import androidx.room.paging.LimitOffsetPagingSource
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.whatsappclone.core.database.entity.MessageEntity
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MessageDao_Impl(
  __db: RoomDatabase,
) : MessageDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfMessageEntity: EntityInsertionAdapter<MessageEntity>

  private val __preparedStmtOfUpdateStatus: SharedSQLiteStatement

  private val __preparedStmtOfConfirmSent: SharedSQLiteStatement

  private val __preparedStmtOfSoftDelete: SharedSQLiteStatement

  private val __preparedStmtOfSetStarred: SharedSQLiteStatement

  private val __preparedStmtOfDeleteAllForChat: SharedSQLiteStatement

  private val __preparedStmtOfUpdateReactions: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__insertionAdapterOfMessageEntity = object : EntityInsertionAdapter<MessageEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `messages` (`messageId`,`clientMsgId`,`chatId`,`senderId`,`messageType`,`content`,`mediaId`,`mediaUrl`,`mediaThumbnailUrl`,`mediaMimeType`,`mediaSize`,`mediaDuration`,`replyToMessageId`,`status`,`isDeleted`,`deletedForEveryone`,`isStarred`,`latitude`,`longitude`,`reactionsJson`,`timestamp`,`createdAt`,`scheduledAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MessageEntity) {
        statement.bindString(1, entity.messageId)
        statement.bindString(2, entity.clientMsgId)
        statement.bindString(3, entity.chatId)
        statement.bindString(4, entity.senderId)
        statement.bindString(5, entity.messageType)
        val _tmpContent: String? = entity.content
        if (_tmpContent == null) {
          statement.bindNull(6)
        } else {
          statement.bindString(6, _tmpContent)
        }
        val _tmpMediaId: String? = entity.mediaId
        if (_tmpMediaId == null) {
          statement.bindNull(7)
        } else {
          statement.bindString(7, _tmpMediaId)
        }
        val _tmpMediaUrl: String? = entity.mediaUrl
        if (_tmpMediaUrl == null) {
          statement.bindNull(8)
        } else {
          statement.bindString(8, _tmpMediaUrl)
        }
        val _tmpMediaThumbnailUrl: String? = entity.mediaThumbnailUrl
        if (_tmpMediaThumbnailUrl == null) {
          statement.bindNull(9)
        } else {
          statement.bindString(9, _tmpMediaThumbnailUrl)
        }
        val _tmpMediaMimeType: String? = entity.mediaMimeType
        if (_tmpMediaMimeType == null) {
          statement.bindNull(10)
        } else {
          statement.bindString(10, _tmpMediaMimeType)
        }
        val _tmpMediaSize: Long? = entity.mediaSize
        if (_tmpMediaSize == null) {
          statement.bindNull(11)
        } else {
          statement.bindLong(11, _tmpMediaSize)
        }
        val _tmpMediaDuration: Int? = entity.mediaDuration
        if (_tmpMediaDuration == null) {
          statement.bindNull(12)
        } else {
          statement.bindLong(12, _tmpMediaDuration.toLong())
        }
        val _tmpReplyToMessageId: String? = entity.replyToMessageId
        if (_tmpReplyToMessageId == null) {
          statement.bindNull(13)
        } else {
          statement.bindString(13, _tmpReplyToMessageId)
        }
        statement.bindString(14, entity.status)
        val _tmp: Int = if (entity.isDeleted) 1 else 0
        statement.bindLong(15, _tmp.toLong())
        val _tmp_1: Int = if (entity.deletedForEveryone) 1 else 0
        statement.bindLong(16, _tmp_1.toLong())
        val _tmp_2: Int = if (entity.isStarred) 1 else 0
        statement.bindLong(17, _tmp_2.toLong())
        val _tmpLatitude: Double? = entity.latitude
        if (_tmpLatitude == null) {
          statement.bindNull(18)
        } else {
          statement.bindDouble(18, _tmpLatitude)
        }
        val _tmpLongitude: Double? = entity.longitude
        if (_tmpLongitude == null) {
          statement.bindNull(19)
        } else {
          statement.bindDouble(19, _tmpLongitude)
        }
        val _tmpReactionsJson: String? = entity.reactionsJson
        if (_tmpReactionsJson == null) {
          statement.bindNull(20)
        } else {
          statement.bindString(20, _tmpReactionsJson)
        }
        statement.bindLong(21, entity.timestamp)
        statement.bindLong(22, entity.createdAt)
        val _tmpScheduledAt: Long? = entity.scheduledAt
        if (_tmpScheduledAt == null) {
          statement.bindNull(23)
        } else {
          statement.bindLong(23, _tmpScheduledAt)
        }
      }
    }
    this.__preparedStmtOfUpdateStatus = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "UPDATE messages SET status = ? WHERE messageId = ?"
        return _query
      }
    }
    this.__preparedStmtOfConfirmSent = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = """
            |
            |        UPDATE messages 
            |        SET messageId = ?, status = 'sent' 
            |        WHERE clientMsgId = ? AND status IN ('pending', 'sending')
            |        
            """.trimMargin()
        return _query
      }
    }
    this.__preparedStmtOfSoftDelete = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = """
            |
            |        UPDATE messages 
            |        SET isDeleted = 1, deletedForEveryone = ?, content = NULL 
            |        WHERE messageId = ?
            |        
            """.trimMargin()
        return _query
      }
    }
    this.__preparedStmtOfSetStarred = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "UPDATE messages SET isStarred = ? WHERE messageId = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteAllForChat = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM messages WHERE chatId = ?"
        return _query
      }
    }
    this.__preparedStmtOfUpdateReactions = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "UPDATE messages SET reactionsJson = ? WHERE messageId = ?"
        return _query
      }
    }
  }

  public override suspend fun insert(message: MessageEntity): Long = CoroutinesRoom.execute(__db,
      true, object : Callable<Long> {
    public override fun call(): Long {
      __db.beginTransaction()
      try {
        val _result: Long = __insertionAdapterOfMessageEntity.insertAndReturnId(message)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun insertAll(messages: List<MessageEntity>): List<Long> =
      CoroutinesRoom.execute(__db, true, object : Callable<List<Long>> {
    public override fun call(): List<Long> {
      __db.beginTransaction()
      try {
        val _result: List<Long> = __insertionAdapterOfMessageEntity.insertAndReturnIdsList(messages)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun updateStatus(messageId: String, status: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdateStatus.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, status)
      _argIndex = 2
      _stmt.bindString(_argIndex, messageId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfUpdateStatus.release(_stmt)
      }
    }
  })

  public override suspend fun confirmSent(clientMsgId: String, serverMessageId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfConfirmSent.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, serverMessageId)
      _argIndex = 2
      _stmt.bindString(_argIndex, clientMsgId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfConfirmSent.release(_stmt)
      }
    }
  })

  public override suspend fun softDelete(messageId: String, forEveryone: Boolean): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfSoftDelete.acquire()
      var _argIndex: Int = 1
      val _tmp: Int = if (forEveryone) 1 else 0
      _stmt.bindLong(_argIndex, _tmp.toLong())
      _argIndex = 2
      _stmt.bindString(_argIndex, messageId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfSoftDelete.release(_stmt)
      }
    }
  })

  public override suspend fun setStarred(messageId: String, isStarred: Boolean): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfSetStarred.acquire()
      var _argIndex: Int = 1
      val _tmp: Int = if (isStarred) 1 else 0
      _stmt.bindLong(_argIndex, _tmp.toLong())
      _argIndex = 2
      _stmt.bindString(_argIndex, messageId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfSetStarred.release(_stmt)
      }
    }
  })

  public override suspend fun deleteAllForChat(chatId: String): Unit = CoroutinesRoom.execute(__db,
      true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAllForChat.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, chatId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAllForChat.release(_stmt)
      }
    }
  })

  public override suspend fun updateReactions(messageId: String, reactionsJson: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdateReactions.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, reactionsJson)
      _argIndex = 2
      _stmt.bindString(_argIndex, messageId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfUpdateReactions.release(_stmt)
      }
    }
  })

  public override fun pagingSource(chatId: String): PagingSource<Int, MessageEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM messages 
        |        WHERE chatId = ? AND isDeleted = 0 
        |        ORDER BY timestamp DESC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return object : LimitOffsetPagingSource<MessageEntity>(_statement, __db, "messages") {
      protected override fun convertRows(cursor: Cursor): List<MessageEntity> {
        val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(cursor, "messageId")
        val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(cursor, "clientMsgId")
        val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(cursor, "chatId")
        val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(cursor, "senderId")
        val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(cursor, "messageType")
        val _cursorIndexOfContent: Int = getColumnIndexOrThrow(cursor, "content")
        val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(cursor, "mediaId")
        val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(cursor, "mediaUrl")
        val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(cursor,
            "mediaThumbnailUrl")
        val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(cursor, "mediaMimeType")
        val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(cursor, "mediaSize")
        val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(cursor, "mediaDuration")
        val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(cursor, "replyToMessageId")
        val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(cursor, "status")
        val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(cursor, "isDeleted")
        val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(cursor,
            "deletedForEveryone")
        val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(cursor, "isStarred")
        val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(cursor, "latitude")
        val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(cursor, "longitude")
        val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(cursor, "reactionsJson")
        val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(cursor, "timestamp")
        val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(cursor, "createdAt")
        val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(cursor, "scheduledAt")
        val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(cursor.getCount())
        while (cursor.moveToNext()) {
          val _item: MessageEntity
          val _tmpMessageId: String
          _tmpMessageId = cursor.getString(_cursorIndexOfMessageId)
          val _tmpClientMsgId: String
          _tmpClientMsgId = cursor.getString(_cursorIndexOfClientMsgId)
          val _tmpChatId: String
          _tmpChatId = cursor.getString(_cursorIndexOfChatId)
          val _tmpSenderId: String
          _tmpSenderId = cursor.getString(_cursorIndexOfSenderId)
          val _tmpMessageType: String
          _tmpMessageType = cursor.getString(_cursorIndexOfMessageType)
          val _tmpContent: String?
          if (cursor.isNull(_cursorIndexOfContent)) {
            _tmpContent = null
          } else {
            _tmpContent = cursor.getString(_cursorIndexOfContent)
          }
          val _tmpMediaId: String?
          if (cursor.isNull(_cursorIndexOfMediaId)) {
            _tmpMediaId = null
          } else {
            _tmpMediaId = cursor.getString(_cursorIndexOfMediaId)
          }
          val _tmpMediaUrl: String?
          if (cursor.isNull(_cursorIndexOfMediaUrl)) {
            _tmpMediaUrl = null
          } else {
            _tmpMediaUrl = cursor.getString(_cursorIndexOfMediaUrl)
          }
          val _tmpMediaThumbnailUrl: String?
          if (cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
            _tmpMediaThumbnailUrl = null
          } else {
            _tmpMediaThumbnailUrl = cursor.getString(_cursorIndexOfMediaThumbnailUrl)
          }
          val _tmpMediaMimeType: String?
          if (cursor.isNull(_cursorIndexOfMediaMimeType)) {
            _tmpMediaMimeType = null
          } else {
            _tmpMediaMimeType = cursor.getString(_cursorIndexOfMediaMimeType)
          }
          val _tmpMediaSize: Long?
          if (cursor.isNull(_cursorIndexOfMediaSize)) {
            _tmpMediaSize = null
          } else {
            _tmpMediaSize = cursor.getLong(_cursorIndexOfMediaSize)
          }
          val _tmpMediaDuration: Int?
          if (cursor.isNull(_cursorIndexOfMediaDuration)) {
            _tmpMediaDuration = null
          } else {
            _tmpMediaDuration = cursor.getInt(_cursorIndexOfMediaDuration)
          }
          val _tmpReplyToMessageId: String?
          if (cursor.isNull(_cursorIndexOfReplyToMessageId)) {
            _tmpReplyToMessageId = null
          } else {
            _tmpReplyToMessageId = cursor.getString(_cursorIndexOfReplyToMessageId)
          }
          val _tmpStatus: String
          _tmpStatus = cursor.getString(_cursorIndexOfStatus)
          val _tmpIsDeleted: Boolean
          val _tmp: Int
          _tmp = cursor.getInt(_cursorIndexOfIsDeleted)
          _tmpIsDeleted = _tmp != 0
          val _tmpDeletedForEveryone: Boolean
          val _tmp_1: Int
          _tmp_1 = cursor.getInt(_cursorIndexOfDeletedForEveryone)
          _tmpDeletedForEveryone = _tmp_1 != 0
          val _tmpIsStarred: Boolean
          val _tmp_2: Int
          _tmp_2 = cursor.getInt(_cursorIndexOfIsStarred)
          _tmpIsStarred = _tmp_2 != 0
          val _tmpLatitude: Double?
          if (cursor.isNull(_cursorIndexOfLatitude)) {
            _tmpLatitude = null
          } else {
            _tmpLatitude = cursor.getDouble(_cursorIndexOfLatitude)
          }
          val _tmpLongitude: Double?
          if (cursor.isNull(_cursorIndexOfLongitude)) {
            _tmpLongitude = null
          } else {
            _tmpLongitude = cursor.getDouble(_cursorIndexOfLongitude)
          }
          val _tmpReactionsJson: String?
          if (cursor.isNull(_cursorIndexOfReactionsJson)) {
            _tmpReactionsJson = null
          } else {
            _tmpReactionsJson = cursor.getString(_cursorIndexOfReactionsJson)
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = cursor.getLong(_cursorIndexOfTimestamp)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = cursor.getLong(_cursorIndexOfCreatedAt)
          val _tmpScheduledAt: Long?
          if (cursor.isNull(_cursorIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = cursor.getLong(_cursorIndexOfScheduledAt)
          }
          _item =
              MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
          _result.add(_item)
        }
        return _result
      }
    }
  }

  public override fun observeRecentMessages(chatId: String, limit: Int): Flow<List<MessageEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM messages 
        |        WHERE chatId = ? AND isDeleted = 0 
        |        ORDER BY timestamp DESC 
        |        LIMIT ?
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 2)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    _argIndex = 2
    _statement.bindLong(_argIndex, limit.toLong())
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
        }
      }

      protected fun finalize() {
        _statement.release()
      }
    })
  }

  public override suspend fun getById(messageId: String): MessageEntity? {
    val _sql: String = "SELECT * FROM messages WHERE messageId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, messageId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<MessageEntity?> {
      public override fun call(): MessageEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MessageEntity?
          if (_cursor.moveToFirst()) {
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _result =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
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

  public override suspend fun getByClientMsgId(clientMsgId: String): MessageEntity? {
    val _sql: String = "SELECT * FROM messages WHERE clientMsgId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, clientMsgId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<MessageEntity?> {
      public override fun call(): MessageEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MessageEntity?
          if (_cursor.moveToFirst()) {
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _result =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
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

  public override fun observeMediaMessages(chatId: String): Flow<List<MessageEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM messages 
        |        WHERE chatId = ? AND messageType IN ('image', 'video', 'document') 
        |        ORDER BY timestamp DESC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
        }
      }

      protected fun finalize() {
        _statement.release()
      }
    })
  }

  public override fun observeStarredMessages(): Flow<List<MessageEntity>> {
    val _sql: String = "SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
        }
      }

      protected fun finalize() {
        _statement.release()
      }
    })
  }

  public override suspend fun getAllPendingMessages(): List<MessageEntity> {
    val _sql: String = "SELECT * FROM messages WHERE status = 'pending' ORDER BY timestamp ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public override suspend fun getDueScheduledMessages(now: Long): List<MessageEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM messages 
        |        WHERE scheduledAt IS NOT NULL AND scheduledAt <= ? AND status = 'scheduled'
        |        ORDER BY scheduledAt ASC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindLong(_argIndex, now)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public override fun observeScheduledMessages(chatId: String): Flow<List<MessageEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM messages 
        |        WHERE chatId = ? AND scheduledAt IS NOT NULL AND status = 'scheduled'
        |        ORDER BY scheduledAt ASC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("messages"), object :
        Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
        }
      }

      protected fun finalize() {
        _statement.release()
      }
    })
  }

  public override suspend fun getAllForChat(chatId: String): List<MessageEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM messages 
        |        WHERE chatId = ? 
        |        ORDER BY timestamp DESC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public override suspend fun searchMessages(query: String, limit: Int): List<MessageEntity> {
    val _sql: String = """
        |
        |        SELECT m.* FROM messages m
        |        JOIN messages_fts fts ON m.rowid = fts.rowid
        |        WHERE messages_fts MATCH ?
        |          AND m.isDeleted = 0
        |        ORDER BY m.timestamp DESC
        |        LIMIT ?
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 2)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, query)
    _argIndex = 2
    _statement.bindLong(_argIndex, limit.toLong())
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public override suspend fun searchMessagesInChat(
    chatId: String,
    query: String,
    limit: Int,
  ): List<MessageEntity> {
    val _sql: String = """
        |
        |        SELECT m.* FROM messages m
        |        JOIN messages_fts fts ON m.rowid = fts.rowid
        |        WHERE messages_fts MATCH ?
        |          AND m.chatId = ?
        |          AND m.isDeleted = 0
        |        ORDER BY m.timestamp DESC
        |        LIMIT ?
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 3)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, query)
    _argIndex = 2
    _statement.bindString(_argIndex, chatId)
    _argIndex = 3
    _statement.bindLong(_argIndex, limit.toLong())
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<List<MessageEntity>> {
      public override fun call(): List<MessageEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfMessageId: Int = getColumnIndexOrThrow(_cursor, "messageId")
          val _cursorIndexOfClientMsgId: Int = getColumnIndexOrThrow(_cursor, "clientMsgId")
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfSenderId: Int = getColumnIndexOrThrow(_cursor, "senderId")
          val _cursorIndexOfMessageType: Int = getColumnIndexOrThrow(_cursor, "messageType")
          val _cursorIndexOfContent: Int = getColumnIndexOrThrow(_cursor, "content")
          val _cursorIndexOfMediaId: Int = getColumnIndexOrThrow(_cursor, "mediaId")
          val _cursorIndexOfMediaUrl: Int = getColumnIndexOrThrow(_cursor, "mediaUrl")
          val _cursorIndexOfMediaThumbnailUrl: Int = getColumnIndexOrThrow(_cursor,
              "mediaThumbnailUrl")
          val _cursorIndexOfMediaMimeType: Int = getColumnIndexOrThrow(_cursor, "mediaMimeType")
          val _cursorIndexOfMediaSize: Int = getColumnIndexOrThrow(_cursor, "mediaSize")
          val _cursorIndexOfMediaDuration: Int = getColumnIndexOrThrow(_cursor, "mediaDuration")
          val _cursorIndexOfReplyToMessageId: Int = getColumnIndexOrThrow(_cursor,
              "replyToMessageId")
          val _cursorIndexOfStatus: Int = getColumnIndexOrThrow(_cursor, "status")
          val _cursorIndexOfIsDeleted: Int = getColumnIndexOrThrow(_cursor, "isDeleted")
          val _cursorIndexOfDeletedForEveryone: Int = getColumnIndexOrThrow(_cursor,
              "deletedForEveryone")
          val _cursorIndexOfIsStarred: Int = getColumnIndexOrThrow(_cursor, "isStarred")
          val _cursorIndexOfLatitude: Int = getColumnIndexOrThrow(_cursor, "latitude")
          val _cursorIndexOfLongitude: Int = getColumnIndexOrThrow(_cursor, "longitude")
          val _cursorIndexOfReactionsJson: Int = getColumnIndexOrThrow(_cursor, "reactionsJson")
          val _cursorIndexOfTimestamp: Int = getColumnIndexOrThrow(_cursor, "timestamp")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfScheduledAt: Int = getColumnIndexOrThrow(_cursor, "scheduledAt")
          val _result: MutableList<MessageEntity> = ArrayList<MessageEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: MessageEntity
            val _tmpMessageId: String
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId)
            val _tmpClientMsgId: String
            _tmpClientMsgId = _cursor.getString(_cursorIndexOfClientMsgId)
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpSenderId: String
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId)
            val _tmpMessageType: String
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType)
            val _tmpContent: String?
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent)
            }
            val _tmpMediaId: String?
            if (_cursor.isNull(_cursorIndexOfMediaId)) {
              _tmpMediaId = null
            } else {
              _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId)
            }
            val _tmpMediaUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl)
            }
            val _tmpMediaThumbnailUrl: String?
            if (_cursor.isNull(_cursorIndexOfMediaThumbnailUrl)) {
              _tmpMediaThumbnailUrl = null
            } else {
              _tmpMediaThumbnailUrl = _cursor.getString(_cursorIndexOfMediaThumbnailUrl)
            }
            val _tmpMediaMimeType: String?
            if (_cursor.isNull(_cursorIndexOfMediaMimeType)) {
              _tmpMediaMimeType = null
            } else {
              _tmpMediaMimeType = _cursor.getString(_cursorIndexOfMediaMimeType)
            }
            val _tmpMediaSize: Long?
            if (_cursor.isNull(_cursorIndexOfMediaSize)) {
              _tmpMediaSize = null
            } else {
              _tmpMediaSize = _cursor.getLong(_cursorIndexOfMediaSize)
            }
            val _tmpMediaDuration: Int?
            if (_cursor.isNull(_cursorIndexOfMediaDuration)) {
              _tmpMediaDuration = null
            } else {
              _tmpMediaDuration = _cursor.getInt(_cursorIndexOfMediaDuration)
            }
            val _tmpReplyToMessageId: String?
            if (_cursor.isNull(_cursorIndexOfReplyToMessageId)) {
              _tmpReplyToMessageId = null
            } else {
              _tmpReplyToMessageId = _cursor.getString(_cursorIndexOfReplyToMessageId)
            }
            val _tmpStatus: String
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus)
            val _tmpIsDeleted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted)
            _tmpIsDeleted = _tmp != 0
            val _tmpDeletedForEveryone: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfDeletedForEveryone)
            _tmpDeletedForEveryone = _tmp_1 != 0
            val _tmpIsStarred: Boolean
            val _tmp_2: Int
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStarred)
            _tmpIsStarred = _tmp_2 != 0
            val _tmpLatitude: Double?
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude)
            }
            val _tmpLongitude: Double?
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude)
            }
            val _tmpReactionsJson: String?
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson)
            }
            val _tmpTimestamp: Long
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpScheduledAt: Long?
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmpScheduledAt = null
            } else {
              _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt)
            }
            _item =
                MessageEntity(_tmpMessageId,_tmpClientMsgId,_tmpChatId,_tmpSenderId,_tmpMessageType,_tmpContent,_tmpMediaId,_tmpMediaUrl,_tmpMediaThumbnailUrl,_tmpMediaMimeType,_tmpMediaSize,_tmpMediaDuration,_tmpReplyToMessageId,_tmpStatus,_tmpIsDeleted,_tmpDeletedForEveryone,_tmpIsStarred,_tmpLatitude,_tmpLongitude,_tmpReactionsJson,_tmpTimestamp,_tmpCreatedAt,_tmpScheduledAt)
            _result.add(_item)
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public override suspend fun getReactionsJson(messageId: String): String? {
    val _sql: String = "SELECT reactionsJson FROM messages WHERE messageId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, messageId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<String?> {
      public override fun call(): String? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _result: String?
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null
            } else {
              _result = _cursor.getString(0)
            }
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

  public override suspend fun getMediaSizeForChat(chatId: String): Long {
    val _sql: String = "SELECT COALESCE(SUM(mediaSize), 0) FROM messages WHERE chatId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<Long> {
      public override fun call(): Long {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _result: Long
          if (_cursor.moveToFirst()) {
            val _tmp: Long
            _tmp = _cursor.getLong(0)
            _result = _tmp
          } else {
            _result = 0L
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public override suspend fun getMessageCountForChat(chatId: String): Int {
    val _sql: String = "SELECT COUNT(*) FROM messages WHERE chatId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<Int> {
      public override fun call(): Int {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _result: Int
          if (_cursor.moveToFirst()) {
            val _tmp: Int
            _tmp = _cursor.getInt(0)
            _result = _tmp
          } else {
            _result = 0
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
