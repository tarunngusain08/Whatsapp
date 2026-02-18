package com.whatsappclone.core.database.dao

import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.CoroutinesRoom
import androidx.room.CoroutinesRoom.Companion.execute
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.EntityUpsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.relation.ChatWithLastMessage
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class ChatDao_Impl(
  __db: RoomDatabase,
) : ChatDao {
  private val __db: RoomDatabase

  private val __preparedStmtOfUpdateUnreadCount: SharedSQLiteStatement

  private val __preparedStmtOfIncrementUnreadCount: SharedSQLiteStatement

  private val __preparedStmtOfSetMuted: SharedSQLiteStatement

  private val __preparedStmtOfUpdateLastMessage: SharedSQLiteStatement

  private val __preparedStmtOfDeleteAll: SharedSQLiteStatement

  private val __upsertionAdapterOfChatEntity: EntityUpsertionAdapter<ChatEntity>
  init {
    this.__db = __db
    this.__preparedStmtOfUpdateUnreadCount = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "UPDATE chats SET unreadCount = ?, updatedAt = ? WHERE chatId = ?"
        return _query
      }
    }
    this.__preparedStmtOfIncrementUnreadCount = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String =
            "UPDATE chats SET unreadCount = unreadCount + 1, updatedAt = ? WHERE chatId = ?"
        return _query
      }
    }
    this.__preparedStmtOfSetMuted = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "UPDATE chats SET isMuted = ?, updatedAt = ? WHERE chatId = ?"
        return _query
      }
    }
    this.__preparedStmtOfUpdateLastMessage = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = """
            |
            |        UPDATE chats 
            |        SET lastMessageId = ?, 
            |            lastMessagePreview = ?, 
            |            lastMessageTimestamp = ?, 
            |            updatedAt = ? 
            |        WHERE chatId = ?
            |        
            """.trimMargin()
        return _query
      }
    }
    this.__preparedStmtOfDeleteAll = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM chats"
        return _query
      }
    }
    this.__upsertionAdapterOfChatEntity = EntityUpsertionAdapter<ChatEntity>(object :
        EntityInsertionAdapter<ChatEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `chats` (`chatId`,`chatType`,`name`,`description`,`avatarUrl`,`lastMessageId`,`lastMessagePreview`,`lastMessageTimestamp`,`unreadCount`,`isMuted`,`isPinned`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: ChatEntity) {
        statement.bindString(1, entity.chatId)
        statement.bindString(2, entity.chatType)
        val _tmpName: String? = entity.name
        if (_tmpName == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmpName)
        }
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpDescription)
        }
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpAvatarUrl)
        }
        val _tmpLastMessageId: String? = entity.lastMessageId
        if (_tmpLastMessageId == null) {
          statement.bindNull(6)
        } else {
          statement.bindString(6, _tmpLastMessageId)
        }
        val _tmpLastMessagePreview: String? = entity.lastMessagePreview
        if (_tmpLastMessagePreview == null) {
          statement.bindNull(7)
        } else {
          statement.bindString(7, _tmpLastMessagePreview)
        }
        val _tmpLastMessageTimestamp: Long? = entity.lastMessageTimestamp
        if (_tmpLastMessageTimestamp == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpLastMessageTimestamp)
        }
        statement.bindLong(9, entity.unreadCount.toLong())
        val _tmp: Int = if (entity.isMuted) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        val _tmp_1: Int = if (entity.isPinned) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindLong(12, entity.createdAt)
        statement.bindLong(13, entity.updatedAt)
      }
    }, object : EntityDeletionOrUpdateAdapter<ChatEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `chats` SET `chatId` = ?,`chatType` = ?,`name` = ?,`description` = ?,`avatarUrl` = ?,`lastMessageId` = ?,`lastMessagePreview` = ?,`lastMessageTimestamp` = ?,`unreadCount` = ?,`isMuted` = ?,`isPinned` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `chatId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: ChatEntity) {
        statement.bindString(1, entity.chatId)
        statement.bindString(2, entity.chatType)
        val _tmpName: String? = entity.name
        if (_tmpName == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmpName)
        }
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpDescription)
        }
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpAvatarUrl)
        }
        val _tmpLastMessageId: String? = entity.lastMessageId
        if (_tmpLastMessageId == null) {
          statement.bindNull(6)
        } else {
          statement.bindString(6, _tmpLastMessageId)
        }
        val _tmpLastMessagePreview: String? = entity.lastMessagePreview
        if (_tmpLastMessagePreview == null) {
          statement.bindNull(7)
        } else {
          statement.bindString(7, _tmpLastMessagePreview)
        }
        val _tmpLastMessageTimestamp: Long? = entity.lastMessageTimestamp
        if (_tmpLastMessageTimestamp == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpLastMessageTimestamp)
        }
        statement.bindLong(9, entity.unreadCount.toLong())
        val _tmp: Int = if (entity.isMuted) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        val _tmp_1: Int = if (entity.isPinned) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindLong(12, entity.createdAt)
        statement.bindLong(13, entity.updatedAt)
        statement.bindString(14, entity.chatId)
      }
    })
  }

  public override suspend fun updateUnreadCount(
    chatId: String,
    count: Int,
    updatedAt: Long,
  ): Unit = CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdateUnreadCount.acquire()
      var _argIndex: Int = 1
      _stmt.bindLong(_argIndex, count.toLong())
      _argIndex = 2
      _stmt.bindLong(_argIndex, updatedAt)
      _argIndex = 3
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
        __preparedStmtOfUpdateUnreadCount.release(_stmt)
      }
    }
  })

  public override suspend fun incrementUnreadCount(chatId: String, updatedAt: Long): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfIncrementUnreadCount.acquire()
      var _argIndex: Int = 1
      _stmt.bindLong(_argIndex, updatedAt)
      _argIndex = 2
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
        __preparedStmtOfIncrementUnreadCount.release(_stmt)
      }
    }
  })

  public override suspend fun setMuted(
    chatId: String,
    isMuted: Boolean,
    updatedAt: Long,
  ): Unit = CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfSetMuted.acquire()
      var _argIndex: Int = 1
      val _tmp: Int = if (isMuted) 1 else 0
      _stmt.bindLong(_argIndex, _tmp.toLong())
      _argIndex = 2
      _stmt.bindLong(_argIndex, updatedAt)
      _argIndex = 3
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
        __preparedStmtOfSetMuted.release(_stmt)
      }
    }
  })

  public override suspend fun updateLastMessage(
    chatId: String,
    messageId: String,
    preview: String?,
    timestamp: Long,
    updatedAt: Long,
  ): Unit = CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdateLastMessage.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, messageId)
      _argIndex = 2
      if (preview == null) {
        _stmt.bindNull(_argIndex)
      } else {
        _stmt.bindString(_argIndex, preview)
      }
      _argIndex = 3
      _stmt.bindLong(_argIndex, timestamp)
      _argIndex = 4
      _stmt.bindLong(_argIndex, updatedAt)
      _argIndex = 5
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
        __preparedStmtOfUpdateLastMessage.release(_stmt)
      }
    }
  })

  public override suspend fun deleteAll(): Unit = CoroutinesRoom.execute(__db, true, object :
      Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteAll.acquire()
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDeleteAll.release(_stmt)
      }
    }
  })

  public override suspend fun upsert(chat: ChatEntity): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfChatEntity.upsert(chat)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun upsertAll(chats: List<ChatEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfChatEntity.upsert(chats)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun observeChatsWithLastMessage(currentUserId: String):
      Flow<List<ChatWithLastMessage>> {
    val _sql: String = """
        |
        |        SELECT 
        |            c.*,
        |            m.content AS lastMessageText,
        |            m.messageType AS lastMessageType,
        |            m.senderId AS lastMessageSenderId,
        |            u.displayName AS lastMessageSenderName,
        |            pu.displayName AS directChatOtherUserName,
        |            pu.avatarUrl AS directChatOtherUserAvatarUrl
        |        FROM chats c
        |        LEFT JOIN messages m ON c.lastMessageId = m.messageId
        |        LEFT JOIN users u ON m.senderId = u.id
        |        LEFT JOIN chat_participants cp 
        |            ON c.chatId = cp.chatId AND c.chatType = 'direct' AND cp.userId != ?
        |        LEFT JOIN users pu ON cp.userId = pu.id
        |        ORDER BY c.isPinned DESC, c.lastMessageTimestamp DESC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, currentUserId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("chats", "messages", "users",
        "chat_participants"), object : Callable<List<ChatWithLastMessage>> {
      public override fun call(): List<ChatWithLastMessage> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfChatType: Int = getColumnIndexOrThrow(_cursor, "chatType")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfLastMessageId: Int = getColumnIndexOrThrow(_cursor, "lastMessageId")
          val _cursorIndexOfLastMessagePreview: Int = getColumnIndexOrThrow(_cursor,
              "lastMessagePreview")
          val _cursorIndexOfLastMessageTimestamp: Int = getColumnIndexOrThrow(_cursor,
              "lastMessageTimestamp")
          val _cursorIndexOfUnreadCount: Int = getColumnIndexOrThrow(_cursor, "unreadCount")
          val _cursorIndexOfIsMuted: Int = getColumnIndexOrThrow(_cursor, "isMuted")
          val _cursorIndexOfIsPinned: Int = getColumnIndexOrThrow(_cursor, "isPinned")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _cursorIndexOfLastMessageText: Int = getColumnIndexOrThrow(_cursor, "lastMessageText")
          val _cursorIndexOfLastMessageType: Int = getColumnIndexOrThrow(_cursor, "lastMessageType")
          val _cursorIndexOfLastMessageSenderId: Int = getColumnIndexOrThrow(_cursor,
              "lastMessageSenderId")
          val _cursorIndexOfLastMessageSenderName: Int = getColumnIndexOrThrow(_cursor,
              "lastMessageSenderName")
          val _cursorIndexOfDirectChatOtherUserName: Int = getColumnIndexOrThrow(_cursor,
              "directChatOtherUserName")
          val _cursorIndexOfDirectChatOtherUserAvatarUrl: Int = getColumnIndexOrThrow(_cursor,
              "directChatOtherUserAvatarUrl")
          val _result: MutableList<ChatWithLastMessage> =
              ArrayList<ChatWithLastMessage>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: ChatWithLastMessage
            val _tmpLastMessageText: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageText)) {
              _tmpLastMessageText = null
            } else {
              _tmpLastMessageText = _cursor.getString(_cursorIndexOfLastMessageText)
            }
            val _tmpLastMessageType: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageType)) {
              _tmpLastMessageType = null
            } else {
              _tmpLastMessageType = _cursor.getString(_cursorIndexOfLastMessageType)
            }
            val _tmpLastMessageSenderId: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageSenderId)) {
              _tmpLastMessageSenderId = null
            } else {
              _tmpLastMessageSenderId = _cursor.getString(_cursorIndexOfLastMessageSenderId)
            }
            val _tmpLastMessageSenderName: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageSenderName)) {
              _tmpLastMessageSenderName = null
            } else {
              _tmpLastMessageSenderName = _cursor.getString(_cursorIndexOfLastMessageSenderName)
            }
            val _tmpDirectChatOtherUserName: String?
            if (_cursor.isNull(_cursorIndexOfDirectChatOtherUserName)) {
              _tmpDirectChatOtherUserName = null
            } else {
              _tmpDirectChatOtherUserName = _cursor.getString(_cursorIndexOfDirectChatOtherUserName)
            }
            val _tmpDirectChatOtherUserAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfDirectChatOtherUserAvatarUrl)) {
              _tmpDirectChatOtherUserAvatarUrl = null
            } else {
              _tmpDirectChatOtherUserAvatarUrl =
                  _cursor.getString(_cursorIndexOfDirectChatOtherUserAvatarUrl)
            }
            val _tmpChat: ChatEntity
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpChatType: String
            _tmpChatType = _cursor.getString(_cursorIndexOfChatType)
            val _tmpName: String?
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName)
            }
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpLastMessageId: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageId)) {
              _tmpLastMessageId = null
            } else {
              _tmpLastMessageId = _cursor.getString(_cursorIndexOfLastMessageId)
            }
            val _tmpLastMessagePreview: String?
            if (_cursor.isNull(_cursorIndexOfLastMessagePreview)) {
              _tmpLastMessagePreview = null
            } else {
              _tmpLastMessagePreview = _cursor.getString(_cursorIndexOfLastMessagePreview)
            }
            val _tmpLastMessageTimestamp: Long?
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp)
            }
            val _tmpUnreadCount: Int
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount)
            val _tmpIsMuted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsMuted)
            _tmpIsMuted = _tmp != 0
            val _tmpIsPinned: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPinned)
            _tmpIsPinned = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _tmpChat =
                ChatEntity(_tmpChatId,_tmpChatType,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpLastMessageId,_tmpLastMessagePreview,_tmpLastMessageTimestamp,_tmpUnreadCount,_tmpIsMuted,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt)
            _item =
                ChatWithLastMessage(_tmpChat,_tmpLastMessageText,_tmpLastMessageType,_tmpLastMessageSenderId,_tmpLastMessageSenderName,_tmpDirectChatOtherUserName,_tmpDirectChatOtherUserAvatarUrl)
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

  public override suspend fun getChatById(chatId: String): ChatEntity? {
    val _sql: String = "SELECT * FROM chats WHERE chatId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<ChatEntity?> {
      public override fun call(): ChatEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfChatType: Int = getColumnIndexOrThrow(_cursor, "chatType")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfLastMessageId: Int = getColumnIndexOrThrow(_cursor, "lastMessageId")
          val _cursorIndexOfLastMessagePreview: Int = getColumnIndexOrThrow(_cursor,
              "lastMessagePreview")
          val _cursorIndexOfLastMessageTimestamp: Int = getColumnIndexOrThrow(_cursor,
              "lastMessageTimestamp")
          val _cursorIndexOfUnreadCount: Int = getColumnIndexOrThrow(_cursor, "unreadCount")
          val _cursorIndexOfIsMuted: Int = getColumnIndexOrThrow(_cursor, "isMuted")
          val _cursorIndexOfIsPinned: Int = getColumnIndexOrThrow(_cursor, "isPinned")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: ChatEntity?
          if (_cursor.moveToFirst()) {
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpChatType: String
            _tmpChatType = _cursor.getString(_cursorIndexOfChatType)
            val _tmpName: String?
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName)
            }
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpLastMessageId: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageId)) {
              _tmpLastMessageId = null
            } else {
              _tmpLastMessageId = _cursor.getString(_cursorIndexOfLastMessageId)
            }
            val _tmpLastMessagePreview: String?
            if (_cursor.isNull(_cursorIndexOfLastMessagePreview)) {
              _tmpLastMessagePreview = null
            } else {
              _tmpLastMessagePreview = _cursor.getString(_cursorIndexOfLastMessagePreview)
            }
            val _tmpLastMessageTimestamp: Long?
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp)
            }
            val _tmpUnreadCount: Int
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount)
            val _tmpIsMuted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsMuted)
            _tmpIsMuted = _tmp != 0
            val _tmpIsPinned: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPinned)
            _tmpIsPinned = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _result =
                ChatEntity(_tmpChatId,_tmpChatType,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpLastMessageId,_tmpLastMessagePreview,_tmpLastMessageTimestamp,_tmpUnreadCount,_tmpIsMuted,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun observeChat(chatId: String): Flow<ChatEntity?> {
    val _sql: String = "SELECT * FROM chats WHERE chatId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("chats"), object : Callable<ChatEntity?> {
      public override fun call(): ChatEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfChatType: Int = getColumnIndexOrThrow(_cursor, "chatType")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfLastMessageId: Int = getColumnIndexOrThrow(_cursor, "lastMessageId")
          val _cursorIndexOfLastMessagePreview: Int = getColumnIndexOrThrow(_cursor,
              "lastMessagePreview")
          val _cursorIndexOfLastMessageTimestamp: Int = getColumnIndexOrThrow(_cursor,
              "lastMessageTimestamp")
          val _cursorIndexOfUnreadCount: Int = getColumnIndexOrThrow(_cursor, "unreadCount")
          val _cursorIndexOfIsMuted: Int = getColumnIndexOrThrow(_cursor, "isMuted")
          val _cursorIndexOfIsPinned: Int = getColumnIndexOrThrow(_cursor, "isPinned")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: ChatEntity?
          if (_cursor.moveToFirst()) {
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpChatType: String
            _tmpChatType = _cursor.getString(_cursorIndexOfChatType)
            val _tmpName: String?
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName)
            }
            val _tmpDescription: String?
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpLastMessageId: String?
            if (_cursor.isNull(_cursorIndexOfLastMessageId)) {
              _tmpLastMessageId = null
            } else {
              _tmpLastMessageId = _cursor.getString(_cursorIndexOfLastMessageId)
            }
            val _tmpLastMessagePreview: String?
            if (_cursor.isNull(_cursorIndexOfLastMessagePreview)) {
              _tmpLastMessagePreview = null
            } else {
              _tmpLastMessagePreview = _cursor.getString(_cursorIndexOfLastMessagePreview)
            }
            val _tmpLastMessageTimestamp: Long?
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp)
            }
            val _tmpUnreadCount: Int
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount)
            val _tmpIsMuted: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsMuted)
            _tmpIsMuted = _tmp != 0
            val _tmpIsPinned: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPinned)
            _tmpIsPinned = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _result =
                ChatEntity(_tmpChatId,_tmpChatType,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpLastMessageId,_tmpLastMessagePreview,_tmpLastMessageTimestamp,_tmpUnreadCount,_tmpIsMuted,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt)
          } else {
            _result = null
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

  public override suspend fun findDirectChatWithUser(currentUserId: String, otherUserId: String):
      String? {
    val _sql: String = """
        |
        |        SELECT c.chatId FROM chats c
        |        INNER JOIN chat_participants cp1 ON c.chatId = cp1.chatId
        |        INNER JOIN chat_participants cp2 ON c.chatId = cp2.chatId
        |        WHERE c.chatType = 'direct'
        |          AND cp1.userId = ?
        |          AND cp2.userId = ?
        |        LIMIT 1
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 2)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, currentUserId)
    _argIndex = 2
    _statement.bindString(_argIndex, otherUserId)
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

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
