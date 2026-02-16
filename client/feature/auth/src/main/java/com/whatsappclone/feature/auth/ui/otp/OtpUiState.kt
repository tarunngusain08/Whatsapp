package com.whatsappclone.feature.auth.ui.otp

data class OtpUiState(
    val phone: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val resendCountdown: Int = 60,
    val canResend: Boolean = false,
    val isNewUser: Boolean = false
)
