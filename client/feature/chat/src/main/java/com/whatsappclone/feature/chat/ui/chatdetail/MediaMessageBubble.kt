package com.whatsappclone.feature.chat.ui.chatdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.whatsappclone.core.common.util.UrlResolver
import com.whatsappclone.core.ui.components.MessageStatusIcon
import com.whatsappclone.core.ui.theme.WhatsAppColors
import com.whatsappclone.feature.chat.model.MessageUi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaMessageBubble(
    message: MessageUi,
    uploadProgress: Float?,
    isDownloaded: Boolean,
    onImageClick: (MessageUi) -> Unit,
    onVideoClick: (MessageUi) -> Unit,
    onDocumentClick: (MessageUi) -> Unit,
    onDownloadClick: (MessageUi) -> Unit,
    onLongPress: ((MessageUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.75f

    val bubbleColor = if (message.isOwnMessage) {
        WhatsAppColors.SentBubble
    } else {
        WhatsAppColors.ReceivedBubble
    }

    val bubbleShape = remember(message.isOwnMessage) {
        if (message.isOwnMessage) {
            RoundedCornerShape(
                topStart = 12.dp, topEnd = 4.dp,
                bottomStart = 12.dp, bottomEnd = 12.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp, topEnd = 12.dp,
                bottomStart = 12.dp, bottomEnd = 12.dp
            )
        }
    }

    val alignment = if (message.isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isOwnMessage) 48.dp else 6.dp,
                end = if (message.isOwnMessage) 6.dp else 48.dp,
                top = 1.dp,
                bottom = 1.dp
            ),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 0.5.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .widthIn(min = 120.dp, max = maxBubbleWidth)
                .then(
                    if (onLongPress != null) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { onLongPress(message) }
                        )
                    } else Modifier
                )
        ) {
            Column {
                when (message.messageType) {
                    "image" -> ImageContent(
                        message = message,
                        uploadProgress = uploadProgress,
                        isDownloaded = isDownloaded,
                        onImageClick = onImageClick,
                        onDownloadClick = onDownloadClick
                    )

                    "video" -> VideoContent(
                        message = message,
                        uploadProgress = uploadProgress,
                        isDownloaded = isDownloaded,
                        onVideoClick = onVideoClick,
                        onDownloadClick = onDownloadClick
                    )

                    "document" -> DocumentContent(
                        message = message,
                        uploadProgress = uploadProgress,
                        onDocumentClick = onDocumentClick
                    )

                    "audio" -> AudioContent(
                        message = message,
                        uploadProgress = uploadProgress
                    )
                }

                // Caption + timestamp footer
                CaptionAndTimestamp(message = message)
            }
        }
    }
}

// ── Image ────────────────────────────────────────────────────────────────

@Composable
private fun ImageContent(
    message: MessageUi,
    uploadProgress: Float?,
    isDownloaded: Boolean,
    onImageClick: (MessageUi) -> Unit,
    onDownloadClick: (MessageUi) -> Unit
) {
    val thumbnailUrl = UrlResolver.resolve(message.mediaThumbnailUrl ?: message.mediaUrl)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .aspectRatio(4f / 3f)
            .clickable { onImageClick(message) },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .crossfade(200)
                .build(),
            contentDescription = "Image message",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Upload progress overlay
        if (uploadProgress != null && uploadProgress < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhatsAppColors.DownloadOverlay),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { uploadProgress },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // Download indicator
        if (!isDownloaded && uploadProgress == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhatsAppColors.DownloadOverlay)
                    .clickable { onDownloadClick(message) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(8.dp)
                )
            }
        }
    }
}

// ── Video ────────────────────────────────────────────────────────────────

@Composable
private fun VideoContent(
    message: MessageUi,
    uploadProgress: Float?,
    isDownloaded: Boolean,
    onVideoClick: (MessageUi) -> Unit,
    onDownloadClick: (MessageUi) -> Unit
) {
    val thumbnailUrl = UrlResolver.resolve(message.mediaThumbnailUrl ?: message.mediaUrl)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .aspectRatio(16f / 9f)
            .clickable { if (isDownloaded) onVideoClick(message) else onDownloadClick(message) },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .crossfade(200)
                .build(),
            contentDescription = "Video thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Play icon overlay (always shown on video)
        if (uploadProgress == null && isDownloaded) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "Play video",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp)
            )
        }

        // Duration badge at bottom-left
        val durationText = message.content?.takeIf { it.contains(":") }
        if (durationText != null) {
            Text(
                text = durationText,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(WhatsAppColors.DurationBadge, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Upload progress overlay
        if (uploadProgress != null && uploadProgress < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhatsAppColors.DownloadOverlay),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { uploadProgress },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // Download indicator
        if (!isDownloaded && uploadProgress == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhatsAppColors.DownloadOverlay)
                    .clickable { onDownloadClick(message) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(8.dp)
                )
            }
        }
    }
}

// ── Document ─────────────────────────────────────────────────────────────

@Composable
private fun DocumentContent(
    message: MessageUi,
    uploadProgress: Float?,
    onDocumentClick: (MessageUi) -> Unit
) {
    val context = LocalContext.current
    val filename = message.content ?: "Document"
    val fileSize = message.mediaUrl?.let { "" } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDocumentClick(message) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (uploadProgress != null && uploadProgress < 1f) {
                CircularProgressIndicator(
                    progress = { uploadProgress },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.InsertDriveFile,
                    contentDescription = "Document",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = filename,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}

// ── Audio ────────────────────────────────────────────────────────────────

@Composable
private fun AudioContent(
    message: MessageUi,
    uploadProgress: Float?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            if (uploadProgress != null && uploadProgress < 1f) {
                CircularProgressIndicator(
                    progress = { uploadProgress },
                    color = Color.White,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play audio",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Waveform placeholder
        Column(modifier = Modifier.weight(1f)) {
            // Simplified waveform bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val barHeights = remember {
                    List(30) { (8..28).random().dp }
                }
                barHeights.forEach { height ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.content ?: "0:00",
                style = MaterialTheme.typography.labelSmall,
                color = WhatsAppColors.MessageMeta,
                fontSize = 11.sp
            )
        }
    }
}

// ── Caption + Timestamp ──────────────────────────────────────────────────

@Composable
private fun CaptionAndTimestamp(
    message: MessageUi
) {
    val hasCaption = message.messageType in listOf("image", "video") &&
        !message.content.isNullOrBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = if (hasCaption) 6.dp else 2.dp,
                bottom = 5.dp
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {
        if (hasCaption) {
            Text(
                text = message.content ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = message.formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = WhatsAppColors.MessageMeta,
            fontSize = 11.sp
        )

        if (message.isOwnMessage) {
            Spacer(modifier = Modifier.width(3.dp))
            MessageStatusIcon(
                status = message.status,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
