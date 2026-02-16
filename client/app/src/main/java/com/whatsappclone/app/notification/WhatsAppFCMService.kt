package com.whatsappclone.app.notification

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.core.database.dao.MessageDao
import com.whatsappclone.core.database.entity.MessageEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * Firebase Cloud Messaging service that handles incoming push notifications.
 *
 * Behaviour depends on the app's foreground/background state and the
 * currently active chat:
 *
 * | State              | Active chat matches? | Action                      |
 * |--------------------|---------------------|-----------------------------|
 * | Foreground         | Yes (same chat)      | Suppress — user sees it live |
 * | Foreground         | No (different chat)  | In-app banner               |
 * | Background / Killed| —                    | System notification          |
 *
 * All Firebase APIs are guarded with try/catch for safety when
 * `google-services.json` is absent.
 */
@AndroidEntryPoint
class WhatsAppFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var notificationBuilder: NotificationBuilder

    @Inject
    lateinit var activeChatTracker: ActiveChatTracker

    @Inject
    lateinit var inAppNotificationManager: InAppNotificationManager

    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var chatDao: ChatDao

    @Inject
    @Named("appScope")
    lateinit var appScope: CoroutineScope

    // ── Token refresh ────────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        fcmTokenManager.onNewToken(token)
    }

    // ── Message handling ─────────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Push notification received: ${message.data}")

        val data = message.data
        val chatId = data["chatId"] ?: return
        val messageId = data["messageId"] ?: return
        val senderName = data["senderName"] ?: "Unknown"
        val content = data["content"] ?: ""
        val chatType = data["chatType"] ?: "direct"
        val chatName = data["chatName"] ?: senderName
        val senderId = data["senderId"] ?: ""
        val avatarUrl = data["avatarUrl"]
        val messageType = data["messageType"] ?: "text"
        val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

        appScope.launch {
            persistMessageLocally(
                messageId = messageId,
                chatId = chatId,
                senderId = senderId,
                content = content,
                messageType = messageType,
                timestamp = timestamp
            )

            val displayContent = formatDisplayContent(messageType, content)

            when {
                isAppInForeground() && activeChatTracker.isActive(chatId) -> {
                    // User is viewing this exact chat — suppress notification entirely.
                    // The real-time message will appear via WebSocket/Flow.
                    Log.d(TAG, "Suppressing notification — user is in chat $chatId")
                }

                isAppInForeground() -> {
                    // User is in the app but viewing a different screen — show in-app banner.
                    Log.d(TAG, "Showing in-app banner for chat $chatId")
                    incrementUnread(chatId)
                    inAppNotificationManager.show(
                        InAppNotification(
                            chatId = chatId,
                            senderName = senderName,
                            content = displayContent,
                            avatarUrl = avatarUrl,
                            chatType = chatType,
                            timestamp = timestamp
                        )
                    )
                }

                else -> {
                    // App is in background or killed — show a full system notification.
                    Log.d(TAG, "Showing system notification for chat $chatId")
                    incrementUnread(chatId)
                    notificationBuilder.showMessageNotification(
                        chatId = chatId,
                        messageId = messageId,
                        senderName = senderName,
                        content = displayContent,
                        chatType = chatType,
                        chatName = chatName,
                        avatarUrl = avatarUrl,
                        timestamp = timestamp
                    )
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Persists the incoming message in the local Room database so the
     * user sees it when they open the chat, even if the WebSocket missed it.
     */
    private suspend fun persistMessageLocally(
        messageId: String,
        chatId: String,
        senderId: String,
        content: String,
        messageType: String,
        timestamp: Long
    ) {
        try {
            val existing = messageDao.getById(messageId)
            if (existing != null) return

            val entity = MessageEntity(
                messageId = messageId,
                clientMsgId = messageId,
                chatId = chatId,
                senderId = senderId,
                messageType = messageType,
                content = content,
                status = "delivered",
                timestamp = timestamp,
                createdAt = System.currentTimeMillis()
            )
            messageDao.insert(entity)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist push message locally", e)
        }
    }

    /**
     * Increments the unread count for a chat (unless the user is viewing it).
     */
    private suspend fun incrementUnread(chatId: String) {
        try {
            chatDao.incrementUnreadCount(
                chatId = chatId,
                updatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to increment unread count for chat $chatId", e)
        }
    }

    /**
     * Formats the notification body depending on the message type.
     * Plain text is shown as-is; media types get a descriptive label.
     */
    private fun formatDisplayContent(messageType: String, content: String): String {
        return when (messageType) {
            "text" -> content
            "image" -> "\uD83D\uDCF7 Photo"
            "video" -> "\uD83C\uDFA5 Video"
            "audio" -> "\uD83C\uDFA4 Voice message"
            "document" -> "\uD83D\uDCC4 Document"
            "sticker" -> "\uD83E\uDEE8 Sticker"
            "location" -> "\uD83D\uDCCD Location"
            "contact" -> "\uD83D\uDC64 Contact"
            else -> content.ifBlank { "New message" }
        }
    }

    /**
     * Checks whether the app is currently in the foreground by querying
     * the ProcessLifecycleOwner. Returns false if the check fails.
     */
    private fun isAppInForeground(): Boolean {
        return try {
            val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to determine app foreground state", e)
            false
        }
    }

    companion object {
        private const val TAG = "WhatsAppFCMService"
    }
}
