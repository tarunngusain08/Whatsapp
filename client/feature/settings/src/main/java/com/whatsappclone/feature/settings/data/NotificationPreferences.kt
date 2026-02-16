package com.whatsappclone.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationPreferences(
    val messageNotificationsEnabled: Boolean = true,
    val messageTone: String = "Default",
    val messageVibrate: String = "Default",
    val groupNotificationsEnabled: Boolean = true,
    val groupTone: String = "Default",
    val groupVibrate: String = "Default",
    val inAppSounds: Boolean = true,
    val inAppPreview: Boolean = true
)

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

@Singleton
class NotificationPreferencesStore @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private object Keys {
        val MESSAGE_NOTIFICATIONS = booleanPreferencesKey("message_notifications")
        val MESSAGE_TONE = stringPreferencesKey("message_tone")
        val MESSAGE_VIBRATE = stringPreferencesKey("message_vibrate")
        val GROUP_NOTIFICATIONS = booleanPreferencesKey("group_notifications")
        val GROUP_TONE = stringPreferencesKey("group_tone")
        val GROUP_VIBRATE = stringPreferencesKey("group_vibrate")
        val IN_APP_SOUNDS = booleanPreferencesKey("in_app_sounds")
        val IN_APP_PREVIEW = booleanPreferencesKey("in_app_preview")
    }

    val preferences: Flow<NotificationPreferences> = context.notificationDataStore.data.map { prefs ->
        NotificationPreferences(
            messageNotificationsEnabled = prefs[Keys.MESSAGE_NOTIFICATIONS] ?: true,
            messageTone = prefs[Keys.MESSAGE_TONE] ?: "Default",
            messageVibrate = prefs[Keys.MESSAGE_VIBRATE] ?: "Default",
            groupNotificationsEnabled = prefs[Keys.GROUP_NOTIFICATIONS] ?: true,
            groupTone = prefs[Keys.GROUP_TONE] ?: "Default",
            groupVibrate = prefs[Keys.GROUP_VIBRATE] ?: "Default",
            inAppSounds = prefs[Keys.IN_APP_SOUNDS] ?: true,
            inAppPreview = prefs[Keys.IN_APP_PREVIEW] ?: true
        )
    }

    suspend fun updateMessageNotifications(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.MESSAGE_NOTIFICATIONS] = enabled }
    }

    suspend fun updateMessageTone(tone: String) {
        context.notificationDataStore.edit { it[Keys.MESSAGE_TONE] = tone }
    }

    suspend fun updateMessageVibrate(vibrate: String) {
        context.notificationDataStore.edit { it[Keys.MESSAGE_VIBRATE] = vibrate }
    }

    suspend fun updateGroupNotifications(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.GROUP_NOTIFICATIONS] = enabled }
    }

    suspend fun updateGroupTone(tone: String) {
        context.notificationDataStore.edit { it[Keys.GROUP_TONE] = tone }
    }

    suspend fun updateGroupVibrate(vibrate: String) {
        context.notificationDataStore.edit { it[Keys.GROUP_VIBRATE] = vibrate }
    }

    suspend fun updateInAppSounds(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.IN_APP_SOUNDS] = enabled }
    }

    suspend fun updateInAppPreview(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.IN_APP_PREVIEW] = enabled }
    }
}
