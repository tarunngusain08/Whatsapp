package com.whatsappclone.feature.contacts.data

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.relation.ContactWithUser
import kotlinx.coroutines.flow.Flow

interface ContactRepository {

    /**
     * Syncs device contacts with the server.
     * Reads phone contacts via ContentResolver, normalizes to E.164,
     * posts to the server, and upserts the results locally.
     *
     * @return the count of registered users found, wrapped in [AppResult].
     */
    suspend fun syncContacts(): AppResult<Int>

    /**
     * Observes all device contacts that are registered on the platform,
     * joined with their user profile info from Room.
     */
    fun observeRegisteredContacts(): Flow<List<ContactWithUser>>

    /**
     * Searches registered contacts by name or phone number.
     */
    fun searchContacts(query: String): Flow<List<ContactWithUser>>

    /**
     * Searches for a user on the server by phone number.
     * If found, upserts the user and contact into the local DB
     * so they appear in future searches.
     *
     * @return the number of matching users found (0 or 1).
     */
    suspend fun searchByPhone(phone: String): AppResult<Int>
}
