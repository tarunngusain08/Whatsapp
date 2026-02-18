package com.whatsappclone.feature.auth.ui.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.feature.auth.domain.SendOtpUseCase
import com.whatsappclone.feature.auth.domain.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OtpNavigationEvent {
    data object NavigateToMain : OtpNavigationEvent()
    data object NavigateToProfileSetup : OtpNavigationEvent()
    data object NavigateBack : OtpNavigationEvent()
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val sendOtpUseCase: SendOtpUseCase
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle["phone"])
    private val devOtp: String? = savedStateHandle["devOtp"]

    private val _uiState = MutableStateFlow(OtpUiState(phone = phone))
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<OtpNavigationEvent>()
    val navigationEvent: SharedFlow<OtpNavigationEvent> = _navigationEvent.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        startCountdown()
        if (!devOtp.isNullOrBlank()) {
            onOtpChanged(devOtp)
        }
    }

    fun onOtpChanged(otp: String) {
        val filtered = otp.filter { it.isDigit() }.take(Constants.OTP_LENGTH)
        _uiState.update { it.copy(otp = filtered, error = null) }

        if (filtered.length == Constants.OTP_LENGTH) {
            onVerify()
        }
    }

    fun onVerify() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = verifyOtpUseCase(state.phone, state.otp)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNewUser = result.data.isNewUser
                        )
                    }
                    if (result.data.isNewUser) {
                        _navigationEvent.emit(OtpNavigationEvent.NavigateToProfileSetup)
                    } else {
                        _navigationEvent.emit(OtpNavigationEvent.NavigateToMain)
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message, otp = "")
                    }
                }

                is AppResult.Loading -> Unit
            }
        }
    }

    fun onResendOtp() {
        if (!_uiState.value.canResend) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, otp = "") }

            when (val result = sendOtpUseCase(_uiState.value.phone)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    startCountdown()
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

    fun onChangeNumber() {
        viewModelScope.launch {
            _navigationEvent.emit(OtpNavigationEvent.NavigateBack)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                resendCountdown = Constants.OTP_RESEND_SECONDS,
                canResend = false
            )
        }

        countdownJob = viewModelScope.launch {
            var remaining = Constants.OTP_RESEND_SECONDS
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(resendCountdown = remaining) }
            }
            _uiState.update { it.copy(canResend = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
