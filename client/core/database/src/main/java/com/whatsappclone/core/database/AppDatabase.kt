package com.whatsappclone.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whatsappclone.core.database.converter.Converters
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.ChatParticipantDao
import com.whatsappclone.core.database.dao.ContactDao
import com.whatsappclone.core.database.dao.GroupDao
import com.whatsappclone.core.database.dao.MediaDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.ChatEntity
import com.whatsappclone.core.database.entity.ChatParticipantEntity
import com.whatsappclone.core.database.entity.ContactEntity
import com.whatsappclone.core.database.entity.GroupEntity
import com.whatsappclone.core.database.entity.MediaEntity
import com.whatsappclone.core.database.entity.MessageEntity
import com.whatsappclone.core.database.entity.MessageFts
import com.whatsappclone.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        ChatParticipantEntity::class,
        MessageEntity::class,
        GroupEntity::class,
        MediaEntity::class,
        MessageFts::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun chatParticipantDao(): ChatParticipantDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun mediaDao(): MediaDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_participants_userId` ON `chat_participants` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_replyToMessageId` ON `messages` (`replyToMessageId`)")
            }
        }
    }
}
