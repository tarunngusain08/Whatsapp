package com.whatsappclone.core.common.util

object PhoneUtils {
    private val E164_REGEX = Regex("^\\+[1-9]\\d{1,14}$")

    private val ISO_TO_CALLING_CODE = mapOf(
        "US" to "+1", "CA" to "+1", "GB" to "+44", "IN" to "+91",
        "AU" to "+61", "DE" to "+49", "FR" to "+33", "BR" to "+55",
        "JP" to "+81", "CN" to "+86", "RU" to "+7", "KR" to "+82",
        "IT" to "+39", "ES" to "+34", "MX" to "+52", "ID" to "+62",
        "NG" to "+234", "ZA" to "+27", "SA" to "+966", "AE" to "+971",
        "SG" to "+65", "MY" to "+60", "PH" to "+63", "PK" to "+92",
        "BD" to "+880", "EG" to "+20", "TR" to "+90", "TH" to "+66",
        "VN" to "+84", "NL" to "+31", "SE" to "+46", "NO" to "+47",
        "DK" to "+45", "FI" to "+358", "PL" to "+48", "AT" to "+43",
        "CH" to "+41", "BE" to "+32", "IE" to "+353", "PT" to "+351",
        "NZ" to "+64", "AR" to "+54", "CL" to "+56", "CO" to "+57",
        "PE" to "+51", "IL" to "+972", "KE" to "+254", "GH" to "+233",
    )

    fun isValidE164(phone: String): Boolean = E164_REGEX.matches(phone)

    /** Returns the E.164 calling code for an ISO 3166-1 alpha-2 country code, or null if unknown. */
    fun countryCodeForIso(iso: String): String? = ISO_TO_CALLING_CODE[iso.uppercase()]

    fun normalizeToE164(countryCode: String, number: String): String {
        val code = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
        val cleaned = number.replace(Regex("[^\\d]"), "")
        return "$code$cleaned"
    }
}
