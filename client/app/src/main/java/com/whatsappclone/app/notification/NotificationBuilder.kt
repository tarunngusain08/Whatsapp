package com.whatsappclone.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.whatsappclone.app.R
import com.whatsappclone.app.WhatsAppApplication.Companion.CHANNEL_GROUPS
import com.whatsappclone.app.WhatsAppApplication.Companion.CHANNEL_MESSAGES
import com.whatsappclone.core.database.dao.ChatDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds rich Android notifications for incoming messages.
 *
 * Features:
 * - [MessagingStyle] for conversation-like notifications
 * - Grouped by chatId (summary notification per chat)
 * - Deep-link PendingIntent using `whatsapp-clone://chat/{chatId}`
 * - Sender avatar loaded via Coil and converted to Bitmap
 * - Muted chats use silent priority
 * - Inline reply via [RemoteInput]
 * - Mark-as-read action button
 * - Separate channels for direct vs group messages
 */
@Singleton
class NotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val imageLoader: ImageLoader
) {

    /**
     * Show a system notification for an incoming message.
     *
     * @param chatId The chat this message belongs to
     * @param messageId Server-assigned message ID
     * @param senderName Display name of the sender
     * @param content The message text (or a description like "Photo", "Video", etc.)
     * @param chatType "direct" or "group"
     * @param chatName For groups, the group name; for direct chats, same as senderName
     * @param avatarUrl Optional URL for the sender's avatar
     * @param timestamp Message timestamp in millis
     */
    suspend fun showMessageNotification(
        chatId: String,
        messageId: String,
        senderName: String,
        content: String,
        chatType: String,
        chatName: String,
        avatarUrl: String?,
        timestamp: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val isMuted = isChatMuted(chatId)
        val channelId = if (chatType == "group") CHANNEL_GROUPS else CHANNEL_MESSAGES
        val notificationId = chatId.hashCode()

        val avatarBitmap = loadAvatarBitmap(avatarUrl)

        val senderPerson = Person.Builder()
            .setName(senderName)
            .apply {
                avatarBitmap?.let { bmp ->
                    setIcon(IconCompat.createWithBitmap(bmp))
                }
            }
            .build()

        val messagingStyle = restoreOrCreateMessagingStyle(
            notificationManager = notificationManager,
            notificationId = notificationId,
            chatName = chatName,
            isGroup = chatType == "group"
        )

        messagingStyle.addMessage(
            content,
            timestamp,
            senderPerson
        )

        val contentIntent = createDeepLinkPendingIntent(chatId)

        val markAsReadIntent = createMarkAsReadPendingIntent(chatId, notificationId)

        val replyAction = createReplyAction(chatId, notificationId)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY_PREFIX + chatId)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .addAction(markAsReadIntent)
            .addAction(replyAction)
            .apply {
                avatarBitmap?.let { setLargeIcon(it) }

                if (isMuted) {
                    setSilent(true)
                    priority = NotificationCompat.PRIORITY_LOW
                } else {
                    priority = NotificationCompat.PRIORITY_HIGH
                    setDefaults(NotificationCompat.DEFAULT_ALL)
                }
            }

        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(chatName)
            )
            .setGroup(GROUP_KEY_PREFIX + chatId)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .apply {
                if (isMuted) {
                    setSilent(true)
                    priority = NotificationCompat.PRIORITY_LOW
                }
            }
            .build()

        try {
            notificationManager.notify(notificationId, builder.build())
            notificationManager.notify(notificationId + SUMMARY_ID_OFFSET, summaryNotification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission", e)
        }
    }

    /**
     * Cancel all notifications for a specific chat (e.g. when the user opens it).
     */
    fun cancelNotificationsForChat(chatId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val notificationId = chatId.hashCode()
        notificationManager.cancel(notificationId)
        notificationManager.cancel(notificationId + SUMMARY_ID_OFFSET)
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private suspend fun isChatMuted(chatId: String): Boolean {
        return try {
            chatDao.getChatById(chatId)?.isMuted == true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check muted state for chat $chatId", e)
            false
        }
    }

    /**
     * Attempts to restore the existing [NotificationCompat.MessagingStyle]
     * from the active notification so new messages are appended to the
     * conversation thread. Falls back to creating a new one.
     */
    private fun restoreOrCreateMessagingStyle(
        notificationManager: NotificationManager,
        notificationId: Int,
        chatName: String,
        isGroup: Boolean
    ): NotificationCompat.MessagingStyle {
        val activeNotifications = try {
            notificationManager.activeNotifications
        } catch (e: Exception) {
            emptyArray()
        }

        val existing = activeNotifications.firstOrNull { it.id == notificationId }
        if (existing != null) {
            val restored = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(existing.notification)
            if (restored != null) return restored
        }

        val mePerson = Person.Builder()
            .setName("Me")
            .build()

        val style = NotificationCompat.MessagingStyle(mePerson)
        style.setConversationTitle(if (isGroup) chatName else null)
        style.setGroupConversation(isGroup)
        return style
    }

    /**
     * Loads an avatar URL as a [Bitmap] via Coil. Returns null on failure
     * or if the URL is null/blank.
     */
    private suspend fun loadAvatarBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(AVATAR_SIZE_PX)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    result.image.toBitmap()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load avatar for notification", e)
                null
            }
        }
    }

    /**
     * Creates a PendingIntent that deep-links into the chat via
     * `whatsapp-clone://chat/{chatId}`.
     */
    private fun createDeepLinkPendingIntent(chatId: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("whatsapp-clone://chat/$chatId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Creates a "Mark as read" action button on the notification.
     */
    private fun createMarkAsReadPendingIntent(
        chatId: String,
        notificationId: Int
    ): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + MARK_READ_REQUEST_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            "Mark as read",
            pendingIntent
        ).build()
    }

    /**
     * Creates an inline reply action with [RemoteInput].
     */
    private fun createReplyAction(
        chatId: String,
        notificationId: Int
    ): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + REPLY_REQUEST_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            "Reply",
            pendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
    }

    companion object {
        private const val TAG = "NotificationBuilder"
        private const val GROUP_KEY_PREFIX = "com.whatsappclone.CHAT_"
        private const val SUMMARY_ID_OFFSET = 100_000
        private const val MARK_READ_REQUEST_OFFSET = 200_000
        private const val REPLY_REQUEST_OFFSET = 300_000
        private const val AVATAR_SIZE_PX = 128

        const val ACTION_MARK_READ = "com.whatsappclone.ACTION_MARK_READ"
        const val ACTION_REPLY = "com.whatsappclone.ACTION_REPLY"
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
}
