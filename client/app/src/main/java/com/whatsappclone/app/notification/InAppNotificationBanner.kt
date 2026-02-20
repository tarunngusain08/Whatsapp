package com.whatsappclone.app.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.whatsappclone.core.common.util.UrlResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

private const val AUTO_DISMISS_MS = 3000L
private const val ANIMATION_DURATION_MS = 300
private const val SWIPE_DISMISS_THRESHOLD = -40f

/**
 * A composable that overlays a slide-down notification banner at the top
 * of the screen when an in-app notification is emitted.
 *
 * Place this at the **root** of your Scaffold/Surface so it sits above
 * all other content. It handles:
 * - Slide-in from top with animation
 * - Auto-dismiss after 3 seconds
 * - Swipe-up to dismiss manually
 * - Tap to navigate to the chat
 *
 * @param notifications SharedFlow of [InAppNotification] events
 * @param onNotificationTap Called when the user taps the banner (navigate to chat)
 */
@Composable
fun InAppNotificationBanner(
    notifications: SharedFlow<InAppNotification>,
    onNotificationTap: (chatId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentNotification by remember { mutableStateOf<InAppNotification?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        notifications.collect { notification ->
            currentNotification = notification
            visible = true
        }
    }

    // Auto-dismiss after the timeout
    LaunchedEffect(visible, currentNotification) {
        if (visible) {
            delay(AUTO_DISMISS_MS)
            visible = false
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        AnimatedVisibility(
            visible = visible && currentNotification != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(ANIMATION_DURATION_MS)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(ANIMATION_DURATION_MS)
            )
        ) {
            currentNotification?.let { notification ->
                BannerContent(
                    notification = notification,
                    onTap = {
                        visible = false
                        onNotificationTap(notification.chatId)
                    },
                    onSwipeDismiss = {
                        visible = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BannerContent(
    notification: InAppNotification,
    onTap: () -> Unit,
    onSwipeDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < SWIPE_DISMISS_THRESHOLD) {
                        onSwipeDismiss()
                    }
                }
            }
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (!notification.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = UrlResolver.resolve(notification.avatarUrl),
                    contentDescription = "${notification.senderName} avatar",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.senderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
