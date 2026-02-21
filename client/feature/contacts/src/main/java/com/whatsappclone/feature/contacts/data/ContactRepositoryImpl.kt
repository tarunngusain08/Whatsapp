package com.whatsappclone.feature.contacts.data

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.common.util.PhoneUtils
import com.whatsappclone.core.database.dao.ContactDao
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.ContactEntity
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.core.database.relation.ContactWithUser
import com.whatsappclone.core.network.api.UserApi
import com.whatsappclone.core.network.model.dto.ContactSyncRequest
import com.whatsappclone.core.network.model.safeApiCall
import kotlinx.coroutines.flow.Flow
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @ApplicationContext private val context: Context,
    private val userApi: UserApi,
    private val contactDao: ContactDao,
    private val userDao: UserDao
) : ContactRepository {

    override suspend fun syncContacts(): AppResult<Int> {
        return try {
            val deviceContacts = readDeviceContacts()
            Log.d(TAG, "Read ${deviceContacts.size} device contacts")
            if (deviceContacts.isEmpty()) {
                return AppResult.Success(0)
            }

            val phoneNumbers = deviceContacts.map { it.phone }
            Log.d(TAG, "Syncing ${phoneNumbers.size} phone numbers with backend")
            val result = safeApiCall {
                userApi.syncContacts(ContactSyncRequest(phoneNumbers = phoneNumbers))
            }

            when (result) {
                is AppResult.Success -> {
                    val registeredUsers = result.data.registeredUsers
                    Log.d(TAG, "Backend returned ${registeredUsers.size} registered users")
                    val registeredPhoneSet = registeredUsers.associateBy { it.phone }
                    val now = System.currentTimeMillis()

                    val userEntities = registeredUsers.map { dto ->
                        UserEntity(
                            id = dto.id,
                            phone = dto.phone,
                            displayName = dto.displayName ?: dto.phone,
                            statusText = dto.statusText,
                            avatarUrl = dto.avatarUrl,
                            isOnline = dto.isOnline ?: false,
                            lastSeen = null,
                            isBlocked = false,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    userDao.upsertAll(userEntities)

                    val contactEntities = deviceContacts.map { dc ->
                        val matchedUser = registeredPhoneSet[dc.phone]
                        ContactEntity(
                            contactId = dc.contactId,
                            phone = dc.phone,
                            deviceName = dc.name,
                            registeredUserId = matchedUser?.id,
                            updatedAt = now
                        )
                    }
                    contactDao.upsertAll(contactEntities)

                    AppResult.Success(registeredUsers.size)
                }

                is AppResult.Error -> result
                is AppResult.Loading -> AppResult.Error(
                    code = ErrorCode.UNKNOWN,
                    message = "Unexpected loading state"
                )
            }
        } catch (e: Exception) {
            AppResult.Error(
                code = ErrorCode.UNKNOWN,
                message = e.message ?: "Failed to sync contacts",
                cause = e
            )
        }
    }

    override fun observeRegisteredContacts(): Flow<List<ContactWithUser>> =
        contactDao.observeRegisteredContacts()

    override fun searchContacts(query: String): Flow<List<ContactWithUser>> =
        contactDao.searchRegisteredContacts(query)

    override suspend fun searchByPhone(phone: String): AppResult<Int> {
        return try {
            val normalized = normalizePhoneNumber(phone)
                ?: return AppResult.Success(0)

            val result = safeApiCall {
                userApi.syncContacts(ContactSyncRequest(phoneNumbers = listOf(normalized)))
            }

            when (result) {
                is AppResult.Success -> {
                    val registeredUsers = result.data.registeredUsers
                    if (registeredUsers.isEmpty()) return AppResult.Success(0)

                    val now = System.currentTimeMillis()
                    val userEntities = registeredUsers.map { dto ->
                        UserEntity(
                            id = dto.id,
                            phone = dto.phone,
                            displayName = dto.displayName ?: dto.phone,
                            statusText = dto.statusText,
                            avatarUrl = dto.avatarUrl,
                            isOnline = dto.isOnline ?: false,
                            lastSeen = null,
                            isBlocked = false,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    userDao.upsertAll(userEntities)

                    val contactEntities = registeredUsers.map { dto ->
                        ContactEntity(
                            contactId = UUID.nameUUIDFromBytes(
                                dto.phone.toByteArray()
                            ).toString(),
                            phone = dto.phone,
                            deviceName = dto.displayName ?: dto.phone,
                            registeredUserId = dto.id,
                            updatedAt = now
                        )
                    }
                    contactDao.upsertAll(contactEntities)

                    AppResult.Success(registeredUsers.size)
                }
                is AppResult.Error -> result
                is AppResult.Loading -> AppResult.Success(0)
            }
        } catch (e: Exception) {
            AppResult.Error(
                code = ErrorCode.UNKNOWN,
                message = e.message ?: "Failed to search by phone",
                cause = e
            )
        }
    }

    /**
     * Reads contacts from the device via ContentResolver.
     * Groups phone numbers by contact, normalizes to E.164 and deduplicates.
     */
    private fun readDeviceContacts(): List<DeviceContact> {
        val contactMap = mutableMapOf<String, DeviceContactBuilder>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        ) ?: return emptyList()

        cursor.use {
            val contactIdIdx = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            )
            val nameIdx = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val numberIdx = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            while (it.moveToNext()) {
                val contactId = it.getString(contactIdIdx) ?: continue
                val name = it.getString(nameIdx) ?: continue
                val rawNumber = it.getString(numberIdx) ?: continue

                val normalized = normalizePhoneNumber(rawNumber)
                if (normalized != null && PhoneUtils.isValidE164(normalized)) {
                    val builder = contactMap.getOrPut(normalized) {
                        DeviceContactBuilder(
                            contactId = UUID.nameUUIDFromBytes(
                                normalized.toByteArray()
                            ).toString(),
                            name = name,
                            phone = normalized
                        )
                    }
                    if (builder.name.isBlank()) {
                        builder.name = name
                    }
                }
            }
        }

        return contactMap.values.map { builder ->
            DeviceContact(
                contactId = builder.contactId,
                name = builder.name,
                phone = builder.phone
            )
        }
    }

    private fun getDeviceCountryCode(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val iso = tm?.simCountryIso?.uppercase()
            ?: tm?.networkCountryIso?.uppercase()
            ?: java.util.Locale.getDefault().country.uppercase()
        return PhoneUtils.countryCodeForIso(iso) ?: "+91"
    }

    private fun normalizePhoneNumber(rawNumber: String): String? {
        val cleaned = rawNumber.replace(Regex("[\\s\\-().]"), "")
        return if (cleaned.startsWith("+") && PhoneUtils.isValidE164(cleaned)) {
            cleaned
        } else if (cleaned.startsWith("+")) {
            val digits = cleaned.replace(Regex("[^\\d+]"), "")
            if (PhoneUtils.isValidE164(digits)) digits else null
        } else {
            val digitsOnly = cleaned.replace(Regex("[^\\d]"), "")
            if (digitsOnly.length in 7..10) {
                val countryCode = getDeviceCountryCode()
                val withCountryCode = "$countryCode$digitsOnly"
                if (PhoneUtils.isValidE164(withCountryCode)) withCountryCode else null
            } else {
                val withPlus = "+$digitsOnly"
                if (PhoneUtils.isValidE164(withPlus)) withPlus else null
            }
        }
    }

    private data class DeviceContact(
        val contactId: String,
        val name: String,
        val phone: String
    )

    private data class DeviceContactBuilder(
        val contactId: String,
        var name: String,
        val phone: String
    )

    companion object {
        private const val TAG = "ContactRepository"
    }
}
