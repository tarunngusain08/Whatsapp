package com.whatsappclone.app.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing an in-app notification banner shown when the
 * user is in the foreground but viewing a different chat.
 */
data class InAppNotification(
    val chatId: String,
    val senderName: String,
    val content: String,
    val avatarUrl: String? = null,
    val chatType: String = "direct",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages in-app notification banners for messages that arrive while the
 * user is in the foreground but viewing a different chat. The UI layer
 * collects [notifications] and renders a slide-down banner.
 *
 * Backed by a [SharedFlow] so that notifications are fire-and-forget:
 * if no collector is active the event is simply dropped (no backpressure).
 */
@Singleton
class InAppNotificationManager @Inject constructor() {

    private val _notifications = MutableSharedFlow<InAppNotification>(
        extraBufferCapacity = 5
    )

    /** Emits in-app notification events for UI consumption. */
    val notifications: SharedFlow<InAppNotification> = _notifications.asSharedFlow()

    /**
     * Emit a new in-app notification. Call from the FCM service when
     * the message is for a chat different from the active one and the
     * app is in the foreground.
     */
    fun show(notification: InAppNotification) {
        _notifications.tryEmit(notification)
    }
}
