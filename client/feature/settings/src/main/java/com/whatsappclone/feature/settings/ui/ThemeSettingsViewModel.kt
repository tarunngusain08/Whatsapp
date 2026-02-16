package com.whatsappclone.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.feature.settings.data.ThemeMode
import com.whatsappclone.feature.settings.data.ThemePreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeSettingsUiState(
    val selectedTheme: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = true
)

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val themePreferencesStore: ThemePreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    init {
        observeThemeMode()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            themePreferencesStore.themeMode.collect { mode ->
                _uiState.update {
                    it.copy(selectedTheme = mode, isLoading = false)
                }
            }
        }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferencesStore.updateThemeMode(mode)
        }
    }
}
