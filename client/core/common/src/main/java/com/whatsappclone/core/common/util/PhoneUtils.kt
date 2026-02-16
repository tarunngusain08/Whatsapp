package com.whatsappclone.core.common.util

object PhoneUtils {
    private val E164_REGEX = Regex("^\\+[1-9]\\d{1,14}$")

    fun isValidE164(phone: String): Boolean = E164_REGEX.matches(phone)

    fun normalizeToE164(countryCode: String, number: String): String {
        val code = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
        val cleaned = number.replace(Regex("[^\\d]"), "")
        return "$code$cleaned"
    }
}
