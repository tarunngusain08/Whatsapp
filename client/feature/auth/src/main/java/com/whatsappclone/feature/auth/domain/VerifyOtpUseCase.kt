package com.whatsappclone.feature.auth.domain

import com.whatsappclone.core.common.result.AppResult
import com.whatsappclone.core.common.result.ErrorCode
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.network.model.dto.AuthTokenResponse
import com.whatsappclone.feature.auth.data.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(phone: String, otp: String): AppResult<AuthTokenResponse> {
        if (otp.length != Constants.OTP_LENGTH) {
            return AppResult.Error(
                code = ErrorCode.VALIDATION_ERROR,
                message = "OTP must be ${Constants.OTP_LENGTH} digits"
            )
        }
        return authRepository.verifyOtp(phone, otp)
    }
}
