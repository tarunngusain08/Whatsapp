package com.whatsappclone.feature.auth.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.network.api.UserApi
import com.whatsappclone.core.network.model.dto.UpdateProfileRequest
import com.whatsappclone.core.network.model.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetupUiState(
    val displayName: String = "",
    val statusText: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isNameValid: Boolean
        get() = displayName.isNotBlank() && displayName.length <= Constants.MAX_DISPLAY_NAME_LENGTH
}

sealed class ProfileSetupNavigationEvent {
    data object NavigateToMain : ProfileSetupNavigationEvent()
}

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userApi: UserApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ProfileSetupNavigationEvent>()
    val navigationEvent: SharedFlow<ProfileSetupNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onNameChanged(name: String) {
        if (name.length <= Constants.MAX_DISPLAY_NAME_LENGTH) {
            _uiState.update { it.copy(displayName = name, error = null) }
        }
    }

    fun onStatusChanged(status: String) {
        if (status.length <= Constants.MAX_STATUS_TEXT_LENGTH) {
            _uiState.update { it.copy(statusText = status, error = null) }
        }
    }

    fun onDoneClicked() {
        val state = _uiState.value
        if (!state.isNameValid) {
            _uiState.update { it.copy(error = "Please enter your name") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val request = UpdateProfileRequest(
                displayName = state.displayName.trim(),
                statusText = state.statusText.trim().ifEmpty { null }
            )

            when (val result = safeApiCall { userApi.updateProfile(request) }) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvent.emit(ProfileSetupNavigationEvent.NavigateToMain)
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
