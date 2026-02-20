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
        fun create(chatId: String): String = "chat/${Uri.encode(chatId)}"
    }

    // ── Contacts ─────────────────────────────────────────────────────────────────

    data object ContactPicker : AppRoute("contact_picker")

    data object ContactInfo : AppRoute("contact_info/{userId}") {
        fun create(userId: String): String = "contact_info/${Uri.encode(userId)}"
    }

    // ── Groups ───────────────────────────────────────────────────────────────────

    data object NewGroup : AppRoute("new_group")

    data object GroupSetup : AppRoute("group_setup")

    data object GroupInfo : AppRoute("group_info/{chatId}") {
        fun create(chatId: String): String = "group_info/${Uri.encode(chatId)}"
    }

    data object AddParticipants : AppRoute("add_participants/{chatId}") {
        fun create(chatId: String): String = "add_participants/${Uri.encode(chatId)}"
    }

    // ── Media ────────────────────────────────────────────────────────────────────

    data object MediaViewer : AppRoute("media_viewer/{mediaId}?senderName={senderName}&timestamp={timestamp}") {
        fun create(
            mediaId: String,
            senderName: String = "",
            timestamp: String = ""
        ): String = "media_viewer/$mediaId?senderName=${Uri.encode(senderName)}&timestamp=${Uri.encode(timestamp)}"
    }

    data object ImageViewer : AppRoute("image_viewer?url={url}&title={title}") {
        fun create(url: String, title: String = ""): String =
            "image_viewer?url=${Uri.encode(url)}&title=${Uri.encode(title)}"
    }

    // ── Shared Media ─────────────────────────────────────────────────────

    data object SharedMedia : AppRoute("shared_media/{chatId}") {
        fun create(chatId: String): String = "shared_media/${Uri.encode(chatId)}"
    }

    // ── Starred Messages ─────────────────────────────────────────────────

    data object StarredMessages : AppRoute("starred_messages")

    // ── Blocked Contacts ──────────────────────────────────────────────────

    data object BlockedContacts : AppRoute("blocked_contacts")

    // ── Settings ─────────────────────────────────────────────────────────────────

    data object Settings : AppRoute("settings")

    data object ProfileEdit : AppRoute("profile_edit")

    data object PrivacySettings : AppRoute("privacy_settings")

    data object NotificationSettings : AppRoute("notification_settings")

    data object ServerUrlSettings : AppRoute("server_url_settings")

    data object ThemeSettings : AppRoute("theme_settings")

    data object StorageUsage : AppRoute("storage_usage")

    data object Wallpaper : AppRoute("wallpaper/{chatId}") {
        fun create(chatId: String): String = "wallpaper/${Uri.encode(chatId)}"
    }

    // ── Location ──────────────────────────────────────────────────────────────

    data object LocationPicker : AppRoute("location_picker/{chatId}") {
        fun create(chatId: String): String = "location_picker/${Uri.encode(chatId)}"
    }

    // ── Status ─────────────────────────────────────────────────────────────────

    data object StatusList : AppRoute("status_list")

    data object StatusViewer : AppRoute("status_viewer/{userId}?startIndex={startIndex}") {
        fun create(userId: String, startIndex: Int = 0): String =
            "status_viewer/$userId?startIndex=$startIndex"
    }

    data object StatusCreator : AppRoute("status_creator")

    // ── Archived Chats ────────────────────────────────────────────────────────

    data object ArchivedChats : AppRoute("archived_chats")

    // ── Receipt Details ───────────────────────────────────────────────────────

    data object ReceiptDetails : AppRoute("receipt_details/{messageId}") {
        fun create(messageId: String): String = "receipt_details/${Uri.encode(messageId)}"
    }

    // ── Forward ──────────────────────────────────────────────────────────────────

    data object ForwardPicker : AppRoute("forward_picker/{messageId}?content={content}&type={type}") {
        fun create(messageId: String, content: String = "", type: String = "text"): String =
            "forward_picker/$messageId?content=${Uri.encode(content)}&type=${Uri.encode(type)}"
    }

    // ── Calls ────────────────────────────────────────────────────────────────

    data object CallScreen : AppRoute("call/{calleeName}?avatarUrl={avatarUrl}&callType={callType}&calleeUserId={calleeUserId}") {
        fun create(
            calleeName: String,
            avatarUrl: String = "",
            callType: String = "audio",
            calleeUserId: String = ""
        ): String =
            "call/${Uri.encode(calleeName)}?avatarUrl=${Uri.encode(avatarUrl)}&callType=${Uri.encode(callType)}&calleeUserId=${Uri.encode(calleeUserId)}"
    }

    // ── Camera ───────────────────────────────────────────────────────────────

    data object Camera : AppRoute("camera")
}
