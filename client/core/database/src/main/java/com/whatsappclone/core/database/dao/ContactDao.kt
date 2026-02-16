package com.whatsappclone.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.whatsappclone.core.database.relation.ContactWithUser
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query(
        """
        SELECT 
            c.contactId,
            c.phone,
            c.deviceName,
            u.id AS userId,
            u.displayName,
            u.avatarUrl,
            u.statusText,
            u.isOnline
        FROM contacts c
        LEFT JOIN users u ON c.registeredUserId = u.id
        WHERE c.registeredUserId IS NOT NULL
        ORDER BY c.deviceName ASC
        """
    )
    fun observeRegisteredContacts(): Flow<List<ContactWithUser>>

    @Query(
        """
        SELECT 
            c.contactId,
            c.phone,
            c.deviceName,
            u.id AS userId,
            u.displayName,
            u.avatarUrl,
            u.statusText,
            u.isOnline
        FROM contacts c
        LEFT JOIN users u ON c.registeredUserId = u.id
        WHERE c.registeredUserId IS NOT NULL
          AND (c.deviceName LIKE '%' || :query || '%' 
               OR c.phone LIKE '%' || :query || '%'
               OR u.displayName LIKE '%' || :query || '%')
        ORDER BY c.deviceName ASC
        """
    )
    fun searchRegisteredContacts(query: String): Flow<List<ContactWithUser>>

    @Upsert
    suspend fun upsertAll(contacts: List<com.whatsappclone.core.database.entity.ContactEntity>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
