package com.whatsappclone.feature.auth.ui.login

import com.whatsappclone.core.common.util.PhoneUtils

data class LoginUiState(
    val countryCode: String = "+91",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isPhoneValid: Boolean
        get() = phoneNumber.length >= 7

    val fullPhoneNumber: String
        get() = PhoneUtils.normalizeToE164(countryCode, phoneNumber)
}
