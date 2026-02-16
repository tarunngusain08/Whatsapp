package com.whatsappclone.core.common.util

object Constants {
    const val MAX_MESSAGE_LENGTH = 65_536
    const val MAX_DISPLAY_NAME_LENGTH = 64
    const val MAX_STATUS_TEXT_LENGTH = 140
    const val MAX_GROUP_NAME_LENGTH = 100
    const val MAX_GROUP_DESCRIPTION_LENGTH = 512
    const val MAX_GROUP_MEMBERS = 256
    const val OTP_LENGTH = 6
    const val OTP_RESEND_SECONDS = 60
    const val TYPING_DEBOUNCE_MS = 3000L
    const val TYPING_TIMEOUT_MS = 5000L
    const val WS_HEARTBEAT_INTERVAL_MS = 25_000L
    const val WS_PONG_TIMEOUT_MS = 10_000L
    const val WS_MAX_RECONNECT_DELAY_MS = 30_000L
    const val MEDIA_IMAGE_MAX_WIDTH = 1600
    const val MEDIA_IMAGE_QUALITY = 80
    const val MEDIA_CACHE_MAX_AGE_DAYS = 30
    const val CONTACT_SYNC_INTERVAL_HOURS = 24L
}
