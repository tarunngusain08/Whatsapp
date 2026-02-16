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
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
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
public class ChatParticipantDao_Impl(
  __db: RoomDatabase,
) : ChatParticipantDao {
  private val __db: RoomDatabase

  private val __preparedStmtOfDelete: SharedSQLiteStatement

  private val __preparedStmtOfDeleteAllForChat: SharedSQLiteStatement

  private val __upsertionAdapterOfChatParticipantEntity:
      EntityUpsertionAdapter<ChatParticipantEntity>
  init {
    this.__db = __db
    this.__preparedStmtOfDelete = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM chat_participants WHERE chatId = ? AND userId = ?"
        return _query
      }
    }
    this.__preparedStmtOfDeleteAllForChat = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM chat_participants WHERE chatId = ?"
        return _query
      }
    }
    this.__upsertionAdapterOfChatParticipantEntity =
        EntityUpsertionAdapter<ChatParticipantEntity>(object :
        EntityInsertionAdapter<ChatParticipantEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `chat_participants` (`chatId`,`userId`,`role`,`joinedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement,
          entity: ChatParticipantEntity) {
        statement.bindString(1, entity.chatId)
        statement.bindString(2, entity.userId)
        statement.bindString(3, entity.role)
        statement.bindLong(4, entity.joinedAt)
      }
    }, object : EntityDeletionOrUpdateAdapter<ChatParticipantEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `chat_participants` SET `chatId` = ?,`userId` = ?,`role` = ?,`joinedAt` = ? WHERE `chatId` = ? AND `userId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement,
          entity: ChatParticipantEntity) {
        statement.bindString(1, entity.chatId)
        statement.bindString(2, entity.userId)
        statement.bindString(3, entity.role)
        statement.bindLong(4, entity.joinedAt)
        statement.bindString(5, entity.chatId)
        statement.bindString(6, entity.userId)
      }
    })
  }

  public override suspend fun delete(chatId: String, userId: String): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfDelete.acquire()
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, chatId)
      _argIndex = 2
      _stmt.bindString(_argIndex, userId)
      try {
        __db.beginTransaction()
        try {
          _stmt.executeUpdateDelete()
          __db.setTransactionSuccessful()
        } finally {
          __db.endTransaction()
        }
      } finally {
        __preparedStmtOfDelete.release(_stmt)
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

  public override suspend fun upsert(participant: ChatParticipantEntity): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfChatParticipantEntity.upsert(participant)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun upsertAll(participants: List<ChatParticipantEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfChatParticipantEntity.upsert(participants)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun observeParticipants(chatId: String): Flow<List<ChatParticipantEntity>> {
    val _sql: String = "SELECT * FROM chat_participants WHERE chatId = ? ORDER BY joinedAt ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("chat_participants"), object :
        Callable<List<ChatParticipantEntity>> {
      public override fun call(): List<ChatParticipantEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfRole: Int = getColumnIndexOrThrow(_cursor, "role")
          val _cursorIndexOfJoinedAt: Int = getColumnIndexOrThrow(_cursor, "joinedAt")
          val _result: MutableList<ChatParticipantEntity> =
              ArrayList<ChatParticipantEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: ChatParticipantEntity
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpRole: String
            _tmpRole = _cursor.getString(_cursorIndexOfRole)
            val _tmpJoinedAt: Long
            _tmpJoinedAt = _cursor.getLong(_cursorIndexOfJoinedAt)
            _item = ChatParticipantEntity(_tmpChatId,_tmpUserId,_tmpRole,_tmpJoinedAt)
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

  public override suspend fun getParticipants(chatId: String): List<ChatParticipantEntity> {
    val _sql: String = "SELECT * FROM chat_participants WHERE chatId = ? ORDER BY joinedAt ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<List<ChatParticipantEntity>>
        {
      public override fun call(): List<ChatParticipantEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfUserId: Int = getColumnIndexOrThrow(_cursor, "userId")
          val _cursorIndexOfRole: Int = getColumnIndexOrThrow(_cursor, "role")
          val _cursorIndexOfJoinedAt: Int = getColumnIndexOrThrow(_cursor, "joinedAt")
          val _result: MutableList<ChatParticipantEntity> =
              ArrayList<ChatParticipantEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: ChatParticipantEntity
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpUserId: String
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            val _tmpRole: String
            _tmpRole = _cursor.getString(_cursorIndexOfRole)
            val _tmpJoinedAt: Long
            _tmpJoinedAt = _cursor.getLong(_cursorIndexOfJoinedAt)
            _item = ChatParticipantEntity(_tmpChatId,_tmpUserId,_tmpRole,_tmpJoinedAt)
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

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
