package com.whatsappclone.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.feature.auth.domain.SendOtpUseCase
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

sealed class LoginNavigationEvent {
    data class NavigateToOtp(val phone: String, val devOtp: String? = null) : LoginNavigationEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<LoginNavigationEvent>()
    val navigationEvent: SharedFlow<LoginNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onCountryCodeChanged(code: String) {
        _uiState.update { it.copy(countryCode = code, error = null) }
    }

    fun onPhoneChanged(phone: String) {
        val digitsOnly = phone.filter { it.isDigit() }
        _uiState.update { it.copy(phoneNumber = digitsOnly, error = null) }
    }

    fun onContinueClicked() {
        val state = _uiState.value
        if (!state.isPhoneValid) {
            _uiState.update { it.copy(error = "Please enter a valid phone number") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = sendOtpUseCase(state.fullPhoneNumber)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvent.emit(
                        LoginNavigationEvent.NavigateToOtp(
                            phone = state.fullPhoneNumber,
                            devOtp = result.data.otp
                        )
                    )
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
