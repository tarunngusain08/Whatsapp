package com.whatsappclone.core.network.url

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedBaseUrl: String = DEFAULT_BASE_URL

    init {
        dataStore.data
            .map { prefs -> prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
            .onEach { cachedBaseUrl = it }
            .launchIn(scope)
    }

    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    suspend fun setBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = url
        }
    }

    fun getBaseUrl(): String {
        return cachedBaseUrl
    }

    fun getWsUrl(): String {
        return getBaseUrl()
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .replace("/api/v1/", "/ws")
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://ae13-2401-4900-1c43-1191-653b-6fd0-c8c2-1fda.ngrok-free.app/api/v1/"

        private val KEY_BASE_URL = stringPreferencesKey("base_url")
    }
}
