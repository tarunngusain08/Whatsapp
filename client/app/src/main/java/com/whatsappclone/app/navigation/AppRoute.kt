package com.whatsappclone.app.navigation

import android.net.Uri

sealed class AppRoute(val route: String) {

    // ── Onboarding ───────────────────────────────────────────────────────────────

    data object Splash : AppRoute("splash")

    data object Login : AppRoute("login")

    data object OtpVerification : AppRoute("otp_verification/{phone}?devOtp={devOtp}") {
        fun create(phone: String, devOtp: String? = null): String {
            val base = "otp_verification/${Uri.encode(phone)}"
            return if (devOtp != null) "$base?devOtp=${Uri.encode(devOtp)}" else base
        }
    }

    data object ProfileSetup : AppRoute("profile_setup")

    // ── Main ─────────────────────────────────────────────────────────────────────

    data object Main : AppRoute("main")

    // ── Chat ─────────────────────────────────────────────────────────────────────

    data object ChatDetail : AppRoute("chat/{chatId}") {
        fun create(chatId: String): String = "chat/$chatId"
    }

    // ── Contacts ─────────────────────────────────────────────────────────────────

    data object ContactPicker : AppRoute("contact_picker")

    data object ContactInfo : AppRoute("contact_info/{userId}") {
        fun create(userId: String): String = "contact_info/$userId"
    }

    // ── Groups ───────────────────────────────────────────────────────────────────

    data object NewGroup : AppRoute("new_group")

    data object GroupSetup : AppRoute("group_setup")

    data object GroupInfo : AppRoute("group_info/{chatId}") {
        fun create(chatId: String): String = "group_info/$chatId"
    }

    data object AddParticipants : AppRoute("add_participants/{chatId}") {
        fun create(chatId: String): String = "add_participants/$chatId"
    }

    // ── Media ────────────────────────────────────────────────────────────────────

    data object MediaViewer : AppRoute("media_viewer/{mediaId}?senderName={senderName}&timestamp={timestamp}") {
        fun create(
            mediaId: String,
            senderName: String = "",
            timestamp: String = ""
        ): String = "media_viewer/$mediaId?senderName=${Uri.encode(senderName)}&timestamp=${Uri.encode(timestamp)}"
    }

    // ── Settings ─────────────────────────────────────────────────────────────────

    data object Settings : AppRoute("settings")

    data object ProfileEdit : AppRoute("profile_edit")

    data object PrivacySettings : AppRoute("privacy_settings")

    data object NotificationSettings : AppRoute("notification_settings")

    data object ServerUrlSettings : AppRoute("server_url_settings")

    // ── Forward ──────────────────────────────────────────────────────────────────

    data object ForwardPicker : AppRoute("forward_picker/{messageId}") {
        fun create(messageId: String): String = "forward_picker/$messageId"
    }
}
