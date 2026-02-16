package com.whatsappclone.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

@Singleton
class ThemePreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.toThemeMode() ?: ThemeMode.SYSTEM
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    private fun String.toThemeMode(): ThemeMode {
        return try {
            ThemeMode.valueOf(this)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }
}
