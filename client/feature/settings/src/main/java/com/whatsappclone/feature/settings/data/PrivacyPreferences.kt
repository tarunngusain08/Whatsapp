package com.whatsappclone.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class Visibility {
    EVERYONE,
    MY_CONTACTS,
    NOBODY
}

data class PrivacyPreferences(
    val lastSeenVisibility: Visibility = Visibility.EVERYONE,
    val profilePhotoVisibility: Visibility = Visibility.EVERYONE,
    val aboutVisibility: Visibility = Visibility.EVERYONE,
    val readReceipts: Boolean = true,
    val groupsVisibility: Visibility = Visibility.EVERYONE
)

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "privacy_preferences"
)

@Singleton
class PrivacyPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LAST_SEEN_VISIBILITY = stringPreferencesKey("last_seen_visibility")
        val PROFILE_PHOTO_VISIBILITY = stringPreferencesKey("profile_photo_visibility")
        val ABOUT_VISIBILITY = stringPreferencesKey("about_visibility")
        val READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        val GROUPS_VISIBILITY = stringPreferencesKey("groups_visibility")
    }

    val preferences: Flow<PrivacyPreferences> = context.privacyDataStore.data.map { prefs ->
        PrivacyPreferences(
            lastSeenVisibility = prefs[Keys.LAST_SEEN_VISIBILITY]
                ?.toVisibility() ?: Visibility.EVERYONE,
            profilePhotoVisibility = prefs[Keys.PROFILE_PHOTO_VISIBILITY]
                ?.toVisibility() ?: Visibility.EVERYONE,
            aboutVisibility = prefs[Keys.ABOUT_VISIBILITY]
                ?.toVisibility() ?: Visibility.EVERYONE,
            readReceipts = prefs[Keys.READ_RECEIPTS] ?: true,
            groupsVisibility = prefs[Keys.GROUPS_VISIBILITY]
                ?.toVisibility() ?: Visibility.EVERYONE
        )
    }

    suspend fun updateLastSeenVisibility(visibility: Visibility) {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.LAST_SEEN_VISIBILITY] = visibility.name
        }
    }

    suspend fun updateProfilePhotoVisibility(visibility: Visibility) {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.PROFILE_PHOTO_VISIBILITY] = visibility.name
        }
    }

    suspend fun updateAboutVisibility(visibility: Visibility) {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.ABOUT_VISIBILITY] = visibility.name
        }
    }

    suspend fun updateReadReceipts(enabled: Boolean) {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.READ_RECEIPTS] = enabled
        }
    }

    suspend fun updateGroupsVisibility(visibility: Visibility) {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.GROUPS_VISIBILITY] = visibility.name
        }
    }

    private fun String.toVisibility(): Visibility {
        return try {
            Visibility.valueOf(this)
        } catch (_: IllegalArgumentException) {
            Visibility.EVERYONE
        }
    }
}
