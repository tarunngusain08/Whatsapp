package com.whatsappclone.core.network.url

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    suspend fun setBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = url
        }
    }

    fun getBaseUrl(): String {
        return DEFAULT_BASE_URL
    }

    fun getWsUrl(): String {
        return getBaseUrl()
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .replace("/api/v1/", "/ws")
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://dismally-cosmographic-krystal.ngrok-free.dev/api/v1/"

        private val KEY_BASE_URL = stringPreferencesKey("base_url")
    }
}
