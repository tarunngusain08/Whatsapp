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
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.whatsappclone.core.database.entity.GroupEntity
import java.lang.Class
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class GroupDao_Impl(
  __db: RoomDatabase,
) : GroupDao {
  private val __db: RoomDatabase

  private val __upsertionAdapterOfGroupEntity: EntityUpsertionAdapter<GroupEntity>
  init {
    this.__db = __db
    this.__upsertionAdapterOfGroupEntity = EntityUpsertionAdapter<GroupEntity>(object :
        EntityInsertionAdapter<GroupEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `groups` (`chatId`,`name`,`description`,`avatarUrl`,`createdBy`,`isAdminOnly`,`memberCount`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: GroupEntity) {
        statement.bindString(1, entity.chatId)
        statement.bindString(2, entity.name)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmpDescription)
        }
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpAvatarUrl)
        }
        statement.bindString(5, entity.createdBy)
        val _tmp: Int = if (entity.isAdminOnly) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.memberCount.toLong())
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.updatedAt)
      }
    }, object : EntityDeletionOrUpdateAdapter<GroupEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `groups` SET `chatId` = ?,`name` = ?,`description` = ?,`avatarUrl` = ?,`createdBy` = ?,`isAdminOnly` = ?,`memberCount` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `chatId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: GroupEntity) {
        statement.bindString(1, entity.chatId)
        statement.bindString(2, entity.name)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindString(3, _tmpDescription)
        }
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpAvatarUrl)
        }
        statement.bindString(5, entity.createdBy)
        val _tmp: Int = if (entity.isAdminOnly) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.memberCount.toLong())
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.updatedAt)
        statement.bindString(10, entity.chatId)
      }
    })
  }

  public override suspend fun upsert(group: GroupEntity): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfGroupEntity.upsert(group)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun getByChatId(chatId: String): GroupEntity? {
    val _sql: String = "SELECT * FROM `groups` WHERE chatId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<GroupEntity?> {
      public override fun call(): GroupEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfCreatedBy: Int = getColumnIndexOrThrow(_cursor, "createdBy")
          val _cursorIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_cursor, "isAdminOnly")
          val _cursorIndexOfMemberCount: Int = getColumnIndexOrThrow(_cursor, "memberCount")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: GroupEntity?
          if (_cursor.moveToFirst()) {
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
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
            val _tmpCreatedBy: String
            _tmpCreatedBy = _cursor.getString(_cursorIndexOfCreatedBy)
            val _tmpIsAdminOnly: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAdminOnly)
            _tmpIsAdminOnly = _tmp != 0
            val _tmpMemberCount: Int
            _tmpMemberCount = _cursor.getInt(_cursorIndexOfMemberCount)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _result =
                GroupEntity(_tmpChatId,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpCreatedBy,_tmpIsAdminOnly,_tmpMemberCount,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun observeGroup(chatId: String): Flow<GroupEntity?> {
    val _sql: String = "SELECT * FROM `groups` WHERE chatId = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, chatId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("groups"), object : Callable<GroupEntity?>
        {
      public override fun call(): GroupEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfChatId: Int = getColumnIndexOrThrow(_cursor, "chatId")
          val _cursorIndexOfName: Int = getColumnIndexOrThrow(_cursor, "name")
          val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_cursor, "description")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfCreatedBy: Int = getColumnIndexOrThrow(_cursor, "createdBy")
          val _cursorIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_cursor, "isAdminOnly")
          val _cursorIndexOfMemberCount: Int = getColumnIndexOrThrow(_cursor, "memberCount")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: GroupEntity?
          if (_cursor.moveToFirst()) {
            val _tmpChatId: String
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId)
            val _tmpName: String
            _tmpName = _cursor.getString(_cursorIndexOfName)
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
            val _tmpCreatedBy: String
            _tmpCreatedBy = _cursor.getString(_cursorIndexOfCreatedBy)
            val _tmpIsAdminOnly: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsAdminOnly)
            _tmpIsAdminOnly = _tmp != 0
            val _tmpMemberCount: Int
            _tmpMemberCount = _cursor.getInt(_cursorIndexOfMemberCount)
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _result =
                GroupEntity(_tmpChatId,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpCreatedBy,_tmpIsAdminOnly,_tmpMemberCount,_tmpCreatedAt,_tmpUpdatedAt)
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

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
