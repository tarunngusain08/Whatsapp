package com.whatsappclone.core.database

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatDao_Impl
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.ChatParticipantDao_Impl
import com.whatsappclone.core.database.dao.ContactDao
import com.whatsappclone.core.database.dao.ContactDao_Impl
import com.whatsappclone.core.database.dao.GroupDao
import com.whatsappclone.core.database.dao.GroupDao_Impl
import com.whatsappclone.core.database.dao.MediaDao
import com.whatsappclone.core.database.dao.MediaDao_Impl
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.MessageDao_Impl
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.dao.UserDao_Impl
import java.lang.Class
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.`annotation`.processing.Generated
import kotlin.Any
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.Set
import androidx.room.util.FtsTableInfo.Companion.read as ftsTableInfoRead
import androidx.room.util.TableInfo.Companion.read as tableInfoRead

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class AppDatabase_Impl : AppDatabase() {
  private val _userDao: Lazy<UserDao> = lazy {
    UserDao_Impl(this)
  }


  private val _contactDao: Lazy<ContactDao> = lazy {
    ContactDao_Impl(this)
  }


  private val _chatDao: Lazy<ChatDao> = lazy {
    ChatDao_Impl(this)
  }


  private val _chatParticipantDao: Lazy<ChatParticipantDao> = lazy {
    ChatParticipantDao_Impl(this)
  }


  private val _messageDao: Lazy<MessageDao> = lazy {
    MessageDao_Impl(this)
  }


  private val _groupDao: Lazy<GroupDao> = lazy {
    GroupDao_Impl(this)
  }


  private val _mediaDao: Lazy<MediaDao> = lazy {
    MediaDao_Impl(this)
  }


  protected override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
    val _openCallback: SupportSQLiteOpenHelper.Callback = RoomOpenHelper(config, object :
        RoomOpenHelper.Delegate(6) {
      public override fun createAllTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `phone` TEXT NOT NULL, `displayName` TEXT NOT NULL, `statusText` TEXT, `avatarUrl` TEXT, `isOnline` INTEGER NOT NULL, `lastSeen` INTEGER, `isBlocked` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_phone` ON `users` (`phone`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`contactId` TEXT NOT NULL, `phone` TEXT NOT NULL, `deviceName` TEXT NOT NULL, `registeredUserId` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`contactId`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_phone` ON `contacts` (`phone`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`chatId` TEXT NOT NULL, `chatType` TEXT NOT NULL, `name` TEXT, `description` TEXT, `avatarUrl` TEXT, `lastMessageId` TEXT, `lastMessagePreview` TEXT, `lastMessageTimestamp` INTEGER, `unreadCount` INTEGER NOT NULL, `isMuted` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`chatId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chats_lastMessageTimestamp` ON `chats` (`lastMessageTimestamp`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_participants` (`chatId` TEXT NOT NULL, `userId` TEXT NOT NULL, `role` TEXT NOT NULL, `joinedAt` INTEGER NOT NULL, PRIMARY KEY(`chatId`, `userId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_participants_userId` ON `chat_participants` (`userId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`messageId` TEXT NOT NULL, `clientMsgId` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `messageType` TEXT NOT NULL, `content` TEXT, `mediaId` TEXT, `mediaUrl` TEXT, `mediaThumbnailUrl` TEXT, `mediaMimeType` TEXT, `mediaSize` INTEGER, `mediaDuration` INTEGER, `replyToMessageId` TEXT, `status` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedForEveryone` INTEGER NOT NULL, `isStarred` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `reactionsJson` TEXT, `timestamp` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `scheduledAt` INTEGER, PRIMARY KEY(`messageId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_timestamp` ON `messages` (`chatId`, `timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_status` ON `messages` (`chatId`, `status`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_clientMsgId` ON `messages` (`clientMsgId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_replyToMessageId` ON `messages` (`replyToMessageId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `groups` (`chatId` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `avatarUrl` TEXT, `createdBy` TEXT NOT NULL, `isAdminOnly` INTEGER NOT NULL, `memberCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`chatId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `media` (`mediaId` TEXT NOT NULL, `uploaderId` TEXT NOT NULL, `fileType` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `originalFilename` TEXT, `sizeBytes` INTEGER NOT NULL, `width` INTEGER, `height` INTEGER, `durationMs` INTEGER, `storageUrl` TEXT NOT NULL, `thumbnailUrl` TEXT, `localPath` TEXT, `localThumbnailPath` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`mediaId`))")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(`content` TEXT NOT NULL, content=`messages`)")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_BEFORE_UPDATE BEFORE UPDATE ON `messages` BEGIN DELETE FROM `messages_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_BEFORE_DELETE BEFORE DELETE ON `messages` BEGIN DELETE FROM `messages_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_AFTER_UPDATE AFTER UPDATE ON `messages` BEGIN INSERT INTO `messages_fts`(`docid`, `content`) VALUES (NEW.`rowid`, NEW.`content`); END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_AFTER_INSERT AFTER INSERT ON `messages` BEGIN INSERT INTO `messages_fts`(`docid`, `content`) VALUES (NEW.`rowid`, NEW.`content`); END")
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '517bec5e58772fd025fb9cfa4ddb3645')")
      }

      public override fun dropAllTables(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `users`")
        db.execSQL("DROP TABLE IF EXISTS `contacts`")
        db.execSQL("DROP TABLE IF EXISTS `chats`")
        db.execSQL("DROP TABLE IF EXISTS `chat_participants`")
        db.execSQL("DROP TABLE IF EXISTS `messages`")
        db.execSQL("DROP TABLE IF EXISTS `groups`")
        db.execSQL("DROP TABLE IF EXISTS `media`")
        db.execSQL("DROP TABLE IF EXISTS `messages_fts`")
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onDestructiveMigration(db)
          }
        }
      }

      public override fun onCreate(db: SupportSQLiteDatabase) {
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onCreate(db)
          }
        }
      }

      public override fun onOpen(db: SupportSQLiteDatabase) {
        mDatabase = db
        internalInitInvalidationTracker(db)
        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
        if (_callbacks != null) {
          for (_callback: RoomDatabase.Callback in _callbacks) {
            _callback.onOpen(db)
          }
        }
      }

      public override fun onPreMigrate(db: SupportSQLiteDatabase) {
        dropFtsSyncTriggers(db)
      }

      public override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_BEFORE_UPDATE BEFORE UPDATE ON `messages` BEGIN DELETE FROM `messages_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_BEFORE_DELETE BEFORE DELETE ON `messages` BEGIN DELETE FROM `messages_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_AFTER_UPDATE AFTER UPDATE ON `messages` BEGIN INSERT INTO `messages_fts`(`docid`, `content`) VALUES (NEW.`rowid`, NEW.`content`); END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_AFTER_INSERT AFTER INSERT ON `messages` BEGIN INSERT INTO `messages_fts`(`docid`, `content`) VALUES (NEW.`rowid`, NEW.`content`); END")
      }

      public override fun onValidateSchema(db: SupportSQLiteDatabase):
          RoomOpenHelper.ValidationResult {
        val _columnsUsers: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(10)
        _columnsUsers.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("phone", TableInfo.Column("phone", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("displayName", TableInfo.Column("displayName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("statusText", TableInfo.Column("statusText", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("avatarUrl", TableInfo.Column("avatarUrl", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("isOnline", TableInfo.Column("isOnline", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("lastSeen", TableInfo.Column("lastSeen", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("isBlocked", TableInfo.Column("isBlocked", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUsers: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesUsers: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(1)
        _indicesUsers.add(TableInfo.Index("index_users_phone", true, listOf("phone"),
            listOf("ASC")))
        val _infoUsers: TableInfo = TableInfo("users", _columnsUsers, _foreignKeysUsers,
            _indicesUsers)
        val _existingUsers: TableInfo = tableInfoRead(db, "users")
        if (!_infoUsers.equals(_existingUsers)) {
          return RoomOpenHelper.ValidationResult(false, """
              |users(com.whatsappclone.core.database.entity.UserEntity).
              | Expected:
              |""".trimMargin() + _infoUsers + """
              |
              | Found:
              |""".trimMargin() + _existingUsers)
        }
        val _columnsContacts: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(5)
        _columnsContacts.put("contactId", TableInfo.Column("contactId", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsContacts.put("phone", TableInfo.Column("phone", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsContacts.put("deviceName", TableInfo.Column("deviceName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsContacts.put("registeredUserId", TableInfo.Column("registeredUserId", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContacts.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysContacts: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesContacts: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(1)
        _indicesContacts.add(TableInfo.Index("index_contacts_phone", true, listOf("phone"),
            listOf("ASC")))
        val _infoContacts: TableInfo = TableInfo("contacts", _columnsContacts, _foreignKeysContacts,
            _indicesContacts)
        val _existingContacts: TableInfo = tableInfoRead(db, "contacts")
        if (!_infoContacts.equals(_existingContacts)) {
          return RoomOpenHelper.ValidationResult(false, """
              |contacts(com.whatsappclone.core.database.entity.ContactEntity).
              | Expected:
              |""".trimMargin() + _infoContacts + """
              |
              | Found:
              |""".trimMargin() + _existingContacts)
        }
        val _columnsChats: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(14)
        _columnsChats.put("chatId", TableInfo.Column("chatId", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("chatType", TableInfo.Column("chatType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("name", TableInfo.Column("name", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("description", TableInfo.Column("description", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("avatarUrl", TableInfo.Column("avatarUrl", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("lastMessageId", TableInfo.Column("lastMessageId", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("lastMessagePreview", TableInfo.Column("lastMessagePreview", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("lastMessageTimestamp", TableInfo.Column("lastMessageTimestamp",
            "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("unreadCount", TableInfo.Column("unreadCount", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("isMuted", TableInfo.Column("isMuted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("isPinned", TableInfo.Column("isPinned", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("isArchived", TableInfo.Column("isArchived", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChats: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesChats: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(1)
        _indicesChats.add(TableInfo.Index("index_chats_lastMessageTimestamp", false,
            listOf("lastMessageTimestamp"), listOf("ASC")))
        val _infoChats: TableInfo = TableInfo("chats", _columnsChats, _foreignKeysChats,
            _indicesChats)
        val _existingChats: TableInfo = tableInfoRead(db, "chats")
        if (!_infoChats.equals(_existingChats)) {
          return RoomOpenHelper.ValidationResult(false, """
              |chats(com.whatsappclone.core.database.entity.ChatEntity).
              | Expected:
              |""".trimMargin() + _infoChats + """
              |
              | Found:
              |""".trimMargin() + _existingChats)
        }
        val _columnsChatParticipants: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(4)
        _columnsChatParticipants.put("chatId", TableInfo.Column("chatId", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChatParticipants.put("userId", TableInfo.Column("userId", "TEXT", true, 2, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChatParticipants.put("role", TableInfo.Column("role", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChatParticipants.put("joinedAt", TableInfo.Column("joinedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChatParticipants: HashSet<TableInfo.ForeignKey> =
            HashSet<TableInfo.ForeignKey>(0)
        val _indicesChatParticipants: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(1)
        _indicesChatParticipants.add(TableInfo.Index("index_chat_participants_userId", false,
            listOf("userId"), listOf("ASC")))
        val _infoChatParticipants: TableInfo = TableInfo("chat_participants",
            _columnsChatParticipants, _foreignKeysChatParticipants, _indicesChatParticipants)
        val _existingChatParticipants: TableInfo = tableInfoRead(db, "chat_participants")
        if (!_infoChatParticipants.equals(_existingChatParticipants)) {
          return RoomOpenHelper.ValidationResult(false, """
              |chat_participants(com.whatsappclone.core.database.entity.ChatParticipantEntity).
              | Expected:
              |""".trimMargin() + _infoChatParticipants + """
              |
              | Found:
              |""".trimMargin() + _existingChatParticipants)
        }
        val _columnsMessages: HashMap<String, TableInfo.Column> =
            HashMap<String, TableInfo.Column>(23)
        _columnsMessages.put("messageId", TableInfo.Column("messageId", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("clientMsgId", TableInfo.Column("clientMsgId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("chatId", TableInfo.Column("chatId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("senderId", TableInfo.Column("senderId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("messageType", TableInfo.Column("messageType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("content", TableInfo.Column("content", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("mediaId", TableInfo.Column("mediaId", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("mediaUrl", TableInfo.Column("mediaUrl", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("mediaThumbnailUrl", TableInfo.Column("mediaThumbnailUrl", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("mediaMimeType", TableInfo.Column("mediaMimeType", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("mediaSize", TableInfo.Column("mediaSize", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("mediaDuration", TableInfo.Column("mediaDuration", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("replyToMessageId", TableInfo.Column("replyToMessageId", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("isDeleted", TableInfo.Column("isDeleted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("deletedForEveryone", TableInfo.Column("deletedForEveryone", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("isStarred", TableInfo.Column("isStarred", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("latitude", TableInfo.Column("latitude", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("longitude", TableInfo.Column("longitude", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("reactionsJson", TableInfo.Column("reactionsJson", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("scheduledAt", TableInfo.Column("scheduledAt", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMessages: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesMessages: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(5)
        _indicesMessages.add(TableInfo.Index("index_messages_chatId_timestamp", false,
            listOf("chatId", "timestamp"), listOf("ASC", "ASC")))
        _indicesMessages.add(TableInfo.Index("index_messages_chatId_status", false, listOf("chatId",
            "status"), listOf("ASC", "ASC")))
        _indicesMessages.add(TableInfo.Index("index_messages_clientMsgId", true,
            listOf("clientMsgId"), listOf("ASC")))
        _indicesMessages.add(TableInfo.Index("index_messages_senderId", false, listOf("senderId"),
            listOf("ASC")))
        _indicesMessages.add(TableInfo.Index("index_messages_replyToMessageId", false,
            listOf("replyToMessageId"), listOf("ASC")))
        val _infoMessages: TableInfo = TableInfo("messages", _columnsMessages, _foreignKeysMessages,
            _indicesMessages)
        val _existingMessages: TableInfo = tableInfoRead(db, "messages")
        if (!_infoMessages.equals(_existingMessages)) {
          return RoomOpenHelper.ValidationResult(false, """
              |messages(com.whatsappclone.core.database.entity.MessageEntity).
              | Expected:
              |""".trimMargin() + _infoMessages + """
              |
              | Found:
              |""".trimMargin() + _existingMessages)
        }
        val _columnsGroups: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(9)
        _columnsGroups.put("chatId", TableInfo.Column("chatId", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("description", TableInfo.Column("description", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("avatarUrl", TableInfo.Column("avatarUrl", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("createdBy", TableInfo.Column("createdBy", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("isAdminOnly", TableInfo.Column("isAdminOnly", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("memberCount", TableInfo.Column("memberCount", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGroups: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesGroups: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoGroups: TableInfo = TableInfo("groups", _columnsGroups, _foreignKeysGroups,
            _indicesGroups)
        val _existingGroups: TableInfo = tableInfoRead(db, "groups")
        if (!_infoGroups.equals(_existingGroups)) {
          return RoomOpenHelper.ValidationResult(false, """
              |groups(com.whatsappclone.core.database.entity.GroupEntity).
              | Expected:
              |""".trimMargin() + _infoGroups + """
              |
              | Found:
              |""".trimMargin() + _existingGroups)
        }
        val _columnsMedia: HashMap<String, TableInfo.Column> = HashMap<String, TableInfo.Column>(14)
        _columnsMedia.put("mediaId", TableInfo.Column("mediaId", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("uploaderId", TableInfo.Column("uploaderId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("fileType", TableInfo.Column("fileType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("mimeType", TableInfo.Column("mimeType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("originalFilename", TableInfo.Column("originalFilename", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("sizeBytes", TableInfo.Column("sizeBytes", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("width", TableInfo.Column("width", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("height", TableInfo.Column("height", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("durationMs", TableInfo.Column("durationMs", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("storageUrl", TableInfo.Column("storageUrl", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("thumbnailUrl", TableInfo.Column("thumbnailUrl", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("localPath", TableInfo.Column("localPath", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("localThumbnailPath", TableInfo.Column("localThumbnailPath", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedia.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMedia: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
        val _indicesMedia: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
        val _infoMedia: TableInfo = TableInfo("media", _columnsMedia, _foreignKeysMedia,
            _indicesMedia)
        val _existingMedia: TableInfo = tableInfoRead(db, "media")
        if (!_infoMedia.equals(_existingMedia)) {
          return RoomOpenHelper.ValidationResult(false, """
              |media(com.whatsappclone.core.database.entity.MediaEntity).
              | Expected:
              |""".trimMargin() + _infoMedia + """
              |
              | Found:
              |""".trimMargin() + _existingMedia)
        }
        val _columnsMessagesFts: HashSet<String> = HashSet<String>(1)
        _columnsMessagesFts.add("content")
        val _infoMessagesFts: FtsTableInfo = FtsTableInfo("messages_fts", _columnsMessagesFts,
            "CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(`content` TEXT NOT NULL, content=`messages`)")
        val _existingMessagesFts: FtsTableInfo = ftsTableInfoRead(db, "messages_fts")
        if (!_infoMessagesFts.equals(_existingMessagesFts)) {
          return RoomOpenHelper.ValidationResult(false, """
              |messages_fts(com.whatsappclone.core.database.entity.MessageFts).
              | Expected:
              |""".trimMargin() + _infoMessagesFts + """
              |
              | Found:
              |""".trimMargin() + _existingMessagesFts)
        }
        return RoomOpenHelper.ValidationResult(true, null)
      }
    }, "517bec5e58772fd025fb9cfa4ddb3645", "192b0711b6e3c38951a13c8e7fbee914")
    val _sqliteConfig: SupportSQLiteOpenHelper.Configuration =
        SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build()
    val _helper: SupportSQLiteOpenHelper = config.sqliteOpenHelperFactory.create(_sqliteConfig)
    return _helper
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: HashMap<String, String> = HashMap<String, String>(1)
    _shadowTablesMap.put("messages_fts", "messages")
    val _viewTables: HashMap<String, Set<String>> = HashMap<String, Set<String>>(0)
    return InvalidationTracker(this, _shadowTablesMap, _viewTables,
        "users","contacts","chats","chat_participants","messages","groups","media","messages_fts")
  }

  public override fun clearAllTables() {
    super.assertNotMainThread()
    val _db: SupportSQLiteDatabase = super.openHelper.writableDatabase
    try {
      super.beginTransaction()
      _db.execSQL("DELETE FROM `users`")
      _db.execSQL("DELETE FROM `contacts`")
      _db.execSQL("DELETE FROM `chats`")
      _db.execSQL("DELETE FROM `chat_participants`")
      _db.execSQL("DELETE FROM `messages`")
      _db.execSQL("DELETE FROM `groups`")
      _db.execSQL("DELETE FROM `media`")
      _db.execSQL("DELETE FROM `messages_fts`")
      super.setTransactionSuccessful()
    } finally {
      super.endTransaction()
      _db.query("PRAGMA wal_checkpoint(FULL)").close()
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM")
      }
    }
  }

  protected override fun getRequiredTypeConverters(): Map<Class<out Any>, List<Class<out Any>>> {
    val _typeConvertersMap: HashMap<Class<out Any>, List<Class<out Any>>> =
        HashMap<Class<out Any>, List<Class<out Any>>>()
    _typeConvertersMap.put(UserDao::class.java, UserDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ContactDao::class.java, ContactDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ChatDao::class.java, ChatDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ChatParticipantDao::class.java,
        ChatParticipantDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MessageDao::class.java, MessageDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(GroupDao::class.java, GroupDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MediaDao::class.java, MediaDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecs(): Set<Class<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: HashSet<Class<out AutoMigrationSpec>> =
        HashSet<Class<out AutoMigrationSpec>>()
    return _autoMigrationSpecsSet
  }

  public override
      fun getAutoMigrations(autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = ArrayList<Migration>()
    return _autoMigrations
  }

  public override fun userDao(): UserDao = _userDao.value

  public override fun contactDao(): ContactDao = _contactDao.value

  public override fun chatDao(): ChatDao = _chatDao.value

  public override fun chatParticipantDao(): ChatParticipantDao = _chatParticipantDao.value

  public override fun messageDao(): MessageDao = _messageDao.value

  public override fun groupDao(): GroupDao = _groupDao.value

  public override fun mediaDao(): MediaDao = _mediaDao.value
}
