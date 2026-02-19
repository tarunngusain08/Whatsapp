package com.whatsappclone.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.whatsappclone.app.notification.NotificationBuilder.Companion.ACTION_MARK_READ
import com.whatsappclone.app.notification.NotificationBuilder.Companion.ACTION_REPLY
import com.whatsappclone.app.notification.NotificationBuilder.Companion.EXTRA_CHAT_ID
import com.whatsappclone.app.notification.NotificationBuilder.Companion.EXTRA_NOTIFICATION_ID
import com.whatsappclone.app.notification.NotificationBuilder.Companion.KEY_TEXT_REPLY
import com.whatsappclone.core.database.dao.ChatDao
import com.whatsappclone.feature.chat.domain.SendMessageUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * BroadcastReceiver that handles notification action buttons:
 * - **Mark as read**: clears unread count for the chat and dismisses the notification.
 * - **Reply**: extracts the inline reply text and sends it (placeholder — actual send
 *   logic should delegate to the message repository in a future phase).
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    @Named("appScope")
    lateinit var appScope: CoroutineScope

    @Inject
    lateinit var chatDao: ChatDao

    @Inject
    lateinit var sendMessageUseCase: SendMessageUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_MARK_READ -> handleMarkAsRead(context, chatId, notificationId)
            ACTION_REPLY -> handleReply(context, intent, chatId, notificationId)
        }
    }

    private fun handleMarkAsRead(context: Context, chatId: String, notificationId: Int) {
        dismissNotification(context, notificationId)

        appScope.launch {
            try {
                chatDao.updateUnreadCount(
                    chatId = chatId,
                    count = 0,
                    updatedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark chat $chatId as read", e)
            }
        }
    }

    private fun handleReply(
        context: Context,
        intent: Intent,
        chatId: String,
        notificationId: Int
    ) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()

        if (replyText.isNullOrBlank()) return

        dismissNotification(context, notificationId)

        appScope.launch {
            try {
                sendMessageUseCase(chatId, replyText)
                chatDao.updateUnreadCount(
                    chatId = chatId,
                    count = 0,
                    updatedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send inline reply for chat $chatId", e)
            }
        }
    }

    private fun dismissNotification(context: Context, notificationId: Int) {
        if (notificationId == -1) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.cancel(notificationId)
        manager.cancel(notificationId + SUMMARY_ID_OFFSET)
    }

    companion object {
        private const val TAG = "NotifActionReceiver"
        private const val SUMMARY_ID_OFFSET = 100_000
    }
}
