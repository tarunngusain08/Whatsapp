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
import androidx.room.util.appendPlaceholders
import androidx.room.util.createCancellationSignal
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.newStringBuilder
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.whatsappclone.core.database.entity.UserEntity
import java.lang.Class
import java.lang.StringBuilder
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
public class UserDao_Impl(
  __db: RoomDatabase,
) : UserDao {
  private val __db: RoomDatabase

  private val __preparedStmtOfUpdatePresence: SharedSQLiteStatement

  private val __preparedStmtOfSetBlocked: SharedSQLiteStatement

  private val __upsertionAdapterOfUserEntity: EntityUpsertionAdapter<UserEntity>
  init {
    this.__db = __db
    this.__preparedStmtOfUpdatePresence = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String =
            "UPDATE users SET isOnline = ?, lastSeen = ?, updatedAt = ? WHERE id = ?"
        return _query
      }
    }
    this.__preparedStmtOfSetBlocked = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "UPDATE users SET isBlocked = ?, updatedAt = ? WHERE id = ?"
        return _query
      }
    }
    this.__upsertionAdapterOfUserEntity = EntityUpsertionAdapter<UserEntity>(object :
        EntityInsertionAdapter<UserEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `users` (`id`,`phone`,`displayName`,`statusText`,`avatarUrl`,`isOnline`,`lastSeen`,`isBlocked`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: UserEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.phone)
        statement.bindString(3, entity.displayName)
        val _tmpStatusText: String? = entity.statusText
        if (_tmpStatusText == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpStatusText)
        }
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpAvatarUrl)
        }
        val _tmp: Int = if (entity.isOnline) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        val _tmpLastSeen: Long? = entity.lastSeen
        if (_tmpLastSeen == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpLastSeen)
        }
        val _tmp_1: Int = if (entity.isBlocked) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        statement.bindLong(9, entity.createdAt)
        statement.bindLong(10, entity.updatedAt)
      }
    }, object : EntityDeletionOrUpdateAdapter<UserEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `users` SET `id` = ?,`phone` = ?,`displayName` = ?,`statusText` = ?,`avatarUrl` = ?,`isOnline` = ?,`lastSeen` = ?,`isBlocked` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: UserEntity) {
        statement.bindString(1, entity.id)
        statement.bindString(2, entity.phone)
        statement.bindString(3, entity.displayName)
        val _tmpStatusText: String? = entity.statusText
        if (_tmpStatusText == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpStatusText)
        }
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(5)
        } else {
          statement.bindString(5, _tmpAvatarUrl)
        }
        val _tmp: Int = if (entity.isOnline) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        val _tmpLastSeen: Long? = entity.lastSeen
        if (_tmpLastSeen == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpLastSeen)
        }
        val _tmp_1: Int = if (entity.isBlocked) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        statement.bindLong(9, entity.createdAt)
        statement.bindLong(10, entity.updatedAt)
        statement.bindString(11, entity.id)
      }
    })
  }

  public override suspend fun updatePresence(
    userId: String,
    isOnline: Boolean,
    lastSeen: Long?,
    updatedAt: Long,
  ): Unit = CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdatePresence.acquire()
      var _argIndex: Int = 1
      val _tmp: Int = if (isOnline) 1 else 0
      _stmt.bindLong(_argIndex, _tmp.toLong())
      _argIndex = 2
      if (lastSeen == null) {
        _stmt.bindNull(_argIndex)
      } else {
        _stmt.bindLong(_argIndex, lastSeen)
      }
      _argIndex = 3
      _stmt.bindLong(_argIndex, updatedAt)
      _argIndex = 4
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
        __preparedStmtOfUpdatePresence.release(_stmt)
      }
    }
  })

  public override suspend fun setBlocked(
    userId: String,
    isBlocked: Boolean,
    updatedAt: Long,
  ): Unit = CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      val _stmt: SupportSQLiteStatement = __preparedStmtOfSetBlocked.acquire()
      var _argIndex: Int = 1
      val _tmp: Int = if (isBlocked) 1 else 0
      _stmt.bindLong(_argIndex, _tmp.toLong())
      _argIndex = 2
      _stmt.bindLong(_argIndex, updatedAt)
      _argIndex = 3
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
        __preparedStmtOfSetBlocked.release(_stmt)
      }
    }
  })

  public override suspend fun upsert(user: UserEntity): Unit = CoroutinesRoom.execute(__db, true,
      object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfUserEntity.upsert(user)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun upsertAll(users: List<UserEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfUserEntity.upsert(users)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun getById(userId: String): UserEntity? {
    val _sql: String = "SELECT * FROM users WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<UserEntity?> {
      public override fun call(): UserEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfDisplayName: Int = getColumnIndexOrThrow(_cursor, "displayName")
          val _cursorIndexOfStatusText: Int = getColumnIndexOrThrow(_cursor, "statusText")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfIsOnline: Int = getColumnIndexOrThrow(_cursor, "isOnline")
          val _cursorIndexOfLastSeen: Int = getColumnIndexOrThrow(_cursor, "lastSeen")
          val _cursorIndexOfIsBlocked: Int = getColumnIndexOrThrow(_cursor, "isBlocked")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: UserEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDisplayName: String
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpIsOnline: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            _tmpIsOnline = _tmp != 0
            val _tmpLastSeen: Long?
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null
            } else {
              _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen)
            }
            val _tmpIsBlocked: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked)
            _tmpIsBlocked = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _result =
                UserEntity(_tmpId,_tmpPhone,_tmpDisplayName,_tmpStatusText,_tmpAvatarUrl,_tmpIsOnline,_tmpLastSeen,_tmpIsBlocked,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override suspend fun getByIds(userIds: List<String>): List<UserEntity> {
    val _stringBuilder: StringBuilder = newStringBuilder()
    _stringBuilder.append("SELECT * FROM users WHERE id IN (")
    val _inputSize: Int = userIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _argCount: Int = 0 + _inputSize
    val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
    var _argIndex: Int = 1
    for (_item: String in userIds) {
      _statement.bindString(_argIndex, _item)
      _argIndex++
    }
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<List<UserEntity>> {
      public override fun call(): List<UserEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfDisplayName: Int = getColumnIndexOrThrow(_cursor, "displayName")
          val _cursorIndexOfStatusText: Int = getColumnIndexOrThrow(_cursor, "statusText")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfIsOnline: Int = getColumnIndexOrThrow(_cursor, "isOnline")
          val _cursorIndexOfLastSeen: Int = getColumnIndexOrThrow(_cursor, "lastSeen")
          val _cursorIndexOfIsBlocked: Int = getColumnIndexOrThrow(_cursor, "isBlocked")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<UserEntity> = ArrayList<UserEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item_1: UserEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDisplayName: String
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpIsOnline: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            _tmpIsOnline = _tmp != 0
            val _tmpLastSeen: Long?
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null
            } else {
              _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen)
            }
            val _tmpIsBlocked: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked)
            _tmpIsBlocked = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _item_1 =
                UserEntity(_tmpId,_tmpPhone,_tmpDisplayName,_tmpStatusText,_tmpAvatarUrl,_tmpIsOnline,_tmpLastSeen,_tmpIsBlocked,_tmpCreatedAt,_tmpUpdatedAt)
            _result.add(_item_1)
          }
          return _result
        } finally {
          _cursor.close()
          _statement.release()
        }
      }
    })
  }

  public override fun observeUser(userId: String): Flow<UserEntity?> {
    val _sql: String = "SELECT * FROM users WHERE id = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, userId)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("users"), object : Callable<UserEntity?> {
      public override fun call(): UserEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfDisplayName: Int = getColumnIndexOrThrow(_cursor, "displayName")
          val _cursorIndexOfStatusText: Int = getColumnIndexOrThrow(_cursor, "statusText")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfIsOnline: Int = getColumnIndexOrThrow(_cursor, "isOnline")
          val _cursorIndexOfLastSeen: Int = getColumnIndexOrThrow(_cursor, "lastSeen")
          val _cursorIndexOfIsBlocked: Int = getColumnIndexOrThrow(_cursor, "isBlocked")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: UserEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDisplayName: String
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpIsOnline: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            _tmpIsOnline = _tmp != 0
            val _tmpLastSeen: Long?
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null
            } else {
              _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen)
            }
            val _tmpIsBlocked: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked)
            _tmpIsBlocked = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _result =
                UserEntity(_tmpId,_tmpPhone,_tmpDisplayName,_tmpStatusText,_tmpAvatarUrl,_tmpIsOnline,_tmpLastSeen,_tmpIsBlocked,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun observeAllUsers(): Flow<List<UserEntity>> {
    val _sql: String = "SELECT * FROM users ORDER BY displayName ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("users"), object :
        Callable<List<UserEntity>> {
      public override fun call(): List<UserEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfDisplayName: Int = getColumnIndexOrThrow(_cursor, "displayName")
          val _cursorIndexOfStatusText: Int = getColumnIndexOrThrow(_cursor, "statusText")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfIsOnline: Int = getColumnIndexOrThrow(_cursor, "isOnline")
          val _cursorIndexOfLastSeen: Int = getColumnIndexOrThrow(_cursor, "lastSeen")
          val _cursorIndexOfIsBlocked: Int = getColumnIndexOrThrow(_cursor, "isBlocked")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<UserEntity> = ArrayList<UserEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: UserEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDisplayName: String
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpIsOnline: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            _tmpIsOnline = _tmp != 0
            val _tmpLastSeen: Long?
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null
            } else {
              _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen)
            }
            val _tmpIsBlocked: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked)
            _tmpIsBlocked = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _item =
                UserEntity(_tmpId,_tmpPhone,_tmpDisplayName,_tmpStatusText,_tmpAvatarUrl,_tmpIsOnline,_tmpLastSeen,_tmpIsBlocked,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override suspend fun getByPhone(phone: String): UserEntity? {
    val _sql: String = "SELECT * FROM users WHERE phone = ?"
    val _statement: RoomSQLiteQuery = acquire(_sql, 1)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, phone)
    val _cancellationSignal: CancellationSignal? = createCancellationSignal()
    return execute(__db, false, _cancellationSignal, object : Callable<UserEntity?> {
      public override fun call(): UserEntity? {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfDisplayName: Int = getColumnIndexOrThrow(_cursor, "displayName")
          val _cursorIndexOfStatusText: Int = getColumnIndexOrThrow(_cursor, "statusText")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfIsOnline: Int = getColumnIndexOrThrow(_cursor, "isOnline")
          val _cursorIndexOfLastSeen: Int = getColumnIndexOrThrow(_cursor, "lastSeen")
          val _cursorIndexOfIsBlocked: Int = getColumnIndexOrThrow(_cursor, "isBlocked")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: UserEntity?
          if (_cursor.moveToFirst()) {
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDisplayName: String
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpIsOnline: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            _tmpIsOnline = _tmp != 0
            val _tmpLastSeen: Long?
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null
            } else {
              _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen)
            }
            val _tmpIsBlocked: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked)
            _tmpIsBlocked = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _result =
                UserEntity(_tmpId,_tmpPhone,_tmpDisplayName,_tmpStatusText,_tmpAvatarUrl,_tmpIsOnline,_tmpLastSeen,_tmpIsBlocked,_tmpCreatedAt,_tmpUpdatedAt)
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

  public override fun observeBlockedUsers(): Flow<List<UserEntity>> {
    val _sql: String = "SELECT * FROM users WHERE isBlocked = 1 ORDER BY displayName ASC"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("users"), object :
        Callable<List<UserEntity>> {
      public override fun call(): List<UserEntity> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfId: Int = getColumnIndexOrThrow(_cursor, "id")
          val _cursorIndexOfPhone: Int = getColumnIndexOrThrow(_cursor, "phone")
          val _cursorIndexOfDisplayName: Int = getColumnIndexOrThrow(_cursor, "displayName")
          val _cursorIndexOfStatusText: Int = getColumnIndexOrThrow(_cursor, "statusText")
          val _cursorIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_cursor, "avatarUrl")
          val _cursorIndexOfIsOnline: Int = getColumnIndexOrThrow(_cursor, "isOnline")
          val _cursorIndexOfLastSeen: Int = getColumnIndexOrThrow(_cursor, "lastSeen")
          val _cursorIndexOfIsBlocked: Int = getColumnIndexOrThrow(_cursor, "isBlocked")
          val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_cursor, "createdAt")
          val _cursorIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_cursor, "updatedAt")
          val _result: MutableList<UserEntity> = ArrayList<UserEntity>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: UserEntity
            val _tmpId: String
            _tmpId = _cursor.getString(_cursorIndexOfId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDisplayName: String
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpIsOnline: Boolean
            val _tmp: Int
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            _tmpIsOnline = _tmp != 0
            val _tmpLastSeen: Long?
            if (_cursor.isNull(_cursorIndexOfLastSeen)) {
              _tmpLastSeen = null
            } else {
              _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen)
            }
            val _tmpIsBlocked: Boolean
            val _tmp_1: Int
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBlocked)
            _tmpIsBlocked = _tmp_1 != 0
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt)
            _item =
                UserEntity(_tmpId,_tmpPhone,_tmpDisplayName,_tmpStatusText,_tmpAvatarUrl,_tmpIsOnline,_tmpLastSeen,_tmpIsBlocked,_tmpCreatedAt,_tmpUpdatedAt)
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

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<Class<*>> = emptyList()
  }
}
