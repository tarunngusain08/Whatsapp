package com.whatsappclone.feature.auth.domain

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.common.util.PhoneUtils
import com.whatsappclone.core.network.model.dto.SendOtpResponse
import com.whatsappclone.feature.auth.data.AuthRepository
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(phone: String): AppResult<SendOtpResponse> {
        if (!PhoneUtils.isValidE164(phone)) {
            return AppResult.Error(
                code = ErrorCode.VALIDATION_ERROR,
                message = "Please enter a valid phone number"
            )
        }
        return authRepository.sendOtp(phone)
    }
}
