package com.whatsappclone.core.database.dao

import android.database.Cursor
import androidx.room.CoroutinesRoom
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.EntityUpsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import com.whatsappclone.core.database.entity.ContactEntity
import com.whatsappclone.core.database.relation.ContactWithUser
import java.lang.Class
import java.util.ArrayList
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class ContactDao_Impl(
  __db: RoomDatabase,
) : ContactDao {
  private val __db: RoomDatabase

  private val __preparedStmtOfDeleteAll: SharedSQLiteStatement

  private val __upsertionAdapterOfContactEntity: EntityUpsertionAdapter<ContactEntity>
  init {
    this.__db = __db
    this.__preparedStmtOfDeleteAll = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "DELETE FROM contacts"
        return _query
      }
    }
    this.__upsertionAdapterOfContactEntity = EntityUpsertionAdapter<ContactEntity>(object :
        EntityInsertionAdapter<ContactEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `contacts` (`contactId`,`phone`,`deviceName`,`registeredUserId`,`updatedAt`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: ContactEntity) {
        statement.bindString(1, entity.contactId)
        statement.bindString(2, entity.phone)
        statement.bindString(3, entity.deviceName)
        val _tmpRegisteredUserId: String? = entity.registeredUserId
        if (_tmpRegisteredUserId == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpRegisteredUserId)
        }
        statement.bindLong(5, entity.updatedAt)
      }
    }, object : EntityDeletionOrUpdateAdapter<ContactEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `contacts` SET `contactId` = ?,`phone` = ?,`deviceName` = ?,`registeredUserId` = ?,`updatedAt` = ? WHERE `contactId` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: ContactEntity) {
        statement.bindString(1, entity.contactId)
        statement.bindString(2, entity.phone)
        statement.bindString(3, entity.deviceName)
        val _tmpRegisteredUserId: String? = entity.registeredUserId
        if (_tmpRegisteredUserId == null) {
          statement.bindNull(4)
        } else {
          statement.bindString(4, _tmpRegisteredUserId)
        }
        statement.bindLong(5, entity.updatedAt)
        statement.bindString(6, entity.contactId)
      }
    })
  }

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

  public override suspend fun upsertAll(contacts: List<ContactEntity>): Unit =
      CoroutinesRoom.execute(__db, true, object : Callable<Unit> {
    public override fun call() {
      __db.beginTransaction()
      try {
        __upsertionAdapterOfContactEntity.upsert(contacts)
        __db.setTransactionSuccessful()
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun observeRegisteredContacts(): Flow<List<ContactWithUser>> {
    val _sql: String = """
        |
        |        SELECT 
        |            c.contactId,
        |            c.phone,
        |            c.deviceName,
        |            u.id AS userId,
        |            u.displayName,
        |            u.avatarUrl,
        |            u.statusText,
        |            u.isOnline
        |        FROM contacts c
        |        LEFT JOIN users u ON c.registeredUserId = u.id
        |        WHERE c.registeredUserId IS NOT NULL
        |        ORDER BY c.deviceName ASC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("contacts", "users"), object :
        Callable<List<ContactWithUser>> {
      public override fun call(): List<ContactWithUser> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfContactId: Int = 0
          val _cursorIndexOfPhone: Int = 1
          val _cursorIndexOfDeviceName: Int = 2
          val _cursorIndexOfUserId: Int = 3
          val _cursorIndexOfDisplayName: Int = 4
          val _cursorIndexOfAvatarUrl: Int = 5
          val _cursorIndexOfStatusText: Int = 6
          val _cursorIndexOfIsOnline: Int = 7
          val _result: MutableList<ContactWithUser> = ArrayList<ContactWithUser>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: ContactWithUser
            val _tmpContactId: String
            _tmpContactId = _cursor.getString(_cursorIndexOfContactId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDeviceName: String
            _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName)
            val _tmpUserId: String?
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            }
            val _tmpDisplayName: String?
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpIsOnline: Boolean?
            val _tmp: Int?
            if (_cursor.isNull(_cursorIndexOfIsOnline)) {
              _tmp = null
            } else {
              _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            }
            _tmpIsOnline = _tmp?.let { it != 0 }
            _item =
                ContactWithUser(_tmpContactId,_tmpPhone,_tmpDeviceName,_tmpUserId,_tmpDisplayName,_tmpAvatarUrl,_tmpStatusText,_tmpIsOnline)
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

  public override fun searchRegisteredContacts(query: String): Flow<List<ContactWithUser>> {
    val _sql: String = """
        |
        |        SELECT 
        |            c.contactId,
        |            c.phone,
        |            c.deviceName,
        |            u.id AS userId,
        |            u.displayName,
        |            u.avatarUrl,
        |            u.statusText,
        |            u.isOnline
        |        FROM contacts c
        |        LEFT JOIN users u ON c.registeredUserId = u.id
        |        WHERE c.registeredUserId IS NOT NULL
        |          AND (c.deviceName LIKE '%' || ? || '%' 
        |               OR c.phone LIKE '%' || ? || '%'
        |               OR u.displayName LIKE '%' || ? || '%')
        |        ORDER BY c.deviceName ASC
        |        
        """.trimMargin()
    val _statement: RoomSQLiteQuery = acquire(_sql, 3)
    var _argIndex: Int = 1
    _statement.bindString(_argIndex, query)
    _argIndex = 2
    _statement.bindString(_argIndex, query)
    _argIndex = 3
    _statement.bindString(_argIndex, query)
    return CoroutinesRoom.createFlow(__db, false, arrayOf("contacts", "users"), object :
        Callable<List<ContactWithUser>> {
      public override fun call(): List<ContactWithUser> {
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
          val _cursorIndexOfContactId: Int = 0
          val _cursorIndexOfPhone: Int = 1
          val _cursorIndexOfDeviceName: Int = 2
          val _cursorIndexOfUserId: Int = 3
          val _cursorIndexOfDisplayName: Int = 4
          val _cursorIndexOfAvatarUrl: Int = 5
          val _cursorIndexOfStatusText: Int = 6
          val _cursorIndexOfIsOnline: Int = 7
          val _result: MutableList<ContactWithUser> = ArrayList<ContactWithUser>(_cursor.getCount())
          while (_cursor.moveToNext()) {
            val _item: ContactWithUser
            val _tmpContactId: String
            _tmpContactId = _cursor.getString(_cursorIndexOfContactId)
            val _tmpPhone: String
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone)
            val _tmpDeviceName: String
            _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName)
            val _tmpUserId: String?
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId)
            }
            val _tmpDisplayName: String?
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName)
            }
            val _tmpAvatarUrl: String?
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl)
            }
            val _tmpStatusText: String?
            if (_cursor.isNull(_cursorIndexOfStatusText)) {
              _tmpStatusText = null
            } else {
              _tmpStatusText = _cursor.getString(_cursorIndexOfStatusText)
            }
            val _tmpIsOnline: Boolean?
            val _tmp: Int?
            if (_cursor.isNull(_cursorIndexOfIsOnline)) {
              _tmp = null
            } else {
              _tmp = _cursor.getInt(_cursorIndexOfIsOnline)
            }
            _tmpIsOnline = _tmp?.let { it != 0 }
            _item =
                ContactWithUser(_tmpContactId,_tmpPhone,_tmpDeviceName,_tmpUserId,_tmpDisplayName,_tmpAvatarUrl,_tmpStatusText,_tmpIsOnline)
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
