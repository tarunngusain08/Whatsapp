package com.whatsappclone.feature.settings.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.database.dao.UserDao
import com.whatsappclone.core.database.entity.UserEntity
import com.whatsappclone.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class SettingsUiState(
    val currentUser: UserEntity? = null,
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val error: String? = null
)

sealed class SettingsNavigationEvent {
    data object NavigateToProfileEdit : SettingsNavigationEvent()
    data object NavigateToAccount : SettingsNavigationEvent()
    data object NavigateToNotifications : SettingsNavigationEvent()
    data object NavigateToPrivacy : SettingsNavigationEvent()
    data object NavigateToServerUrl : SettingsNavigationEvent()
    data object NavigateToLogin : SettingsNavigationEvent()
    data object NavigateBack : SettingsNavigationEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userDao: UserDao,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SettingsNavigationEvent>()
    val navigationEvent: SharedFlow<SettingsNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val currentUserId = encryptedPrefs.getString("current_user_id", null)
        if (currentUserId != null) {
            viewModelScope.launch {
                userDao.observeUser(currentUserId)
                    .filterNotNull()
                    .collect { user ->
                        _uiState.update {
                            it.copy(currentUser = user, isLoading = false)
                        }
                    }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onProfileClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(SettingsNavigationEvent.NavigateToProfileEdit)
        }
    }

    fun onAccountClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(SettingsNavigationEvent.NavigateToAccount)
        }
    }

    fun onNotificationsClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(SettingsNavigationEvent.NavigateToNotifications)
        }
    }

    fun onPrivacyClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(SettingsNavigationEvent.NavigateToPrivacy)
        }
    }

    fun onServerUrlClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(SettingsNavigationEvent.NavigateToServerUrl)
        }
    }

    fun onLogoutClicked() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun onLogoutDismissed() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun onLogoutConfirmed() {
        _uiState.update { it.copy(showLogoutDialog = false, isLoggingOut = true) }

        viewModelScope.launch {
            when (val result = authRepository.logout()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoggingOut = false) }
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToLogin)
                }

                is AppResult.Error -> {
                    // Even if API call fails, still log out locally
                    _uiState.update { it.copy(isLoggingOut = false) }
                    _navigationEvent.emit(SettingsNavigationEvent.NavigateToLogin)
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(SettingsNavigationEvent.NavigateBack)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
