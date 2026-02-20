package com.whatsappclone.feature.media.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.whatsappclone.core.common.util.UrlResolver
import kotlin.math.abs
import kotlin.math.roundToInt

private val TopBarOverlay = Color(0xCC000000)

@Composable
fun MediaViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Immersive black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { uiState.downloadProgress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    )
                    if (uiState.downloadProgress > 0f) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${(uiState.downloadProgress * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            uiState.error != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.retry() }) {
                        Text("Retry", color = Color.White)
                    }
                }
            }

            else -> {
                val entity = uiState.mediaEntity
                val localFile = uiState.localFile

                if (entity != null) {
                    val imageUrl = localFile?.absolutePath ?: entity.storageUrl

                    // Pinch-to-zoom and pan state
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    // Swipe-to-dismiss state
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }
                    var showOverlay by remember { mutableStateOf(true) }

                    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                        offset = if (scale > 1f) {
                            Offset(
                                x = offset.x + panChange.x,
                                y = offset.y + panChange.y
                            )
                        } else {
                            Offset.Zero
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (abs(dragOffsetY) > 200f && scale <= 1f) {
                                            onNavigateBack()
                                        } else {
                                            dragOffsetY = 0f
                                        }
                                    },
                                    onVerticalDrag = { _, dragAmount ->
                                        if (scale <= 1f) {
                                            dragOffsetY += dragAmount
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showOverlay = !showOverlay },
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(
                                    if (localFile != null) localFile
                                    else UrlResolver.resolve(entity.storageUrl)
                                )
                                .crossfade(300)
                                .build(),
                            contentDescription = "Media",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .offset {
                                    IntOffset(
                                        x = offset.x.roundToInt(),
                                        y = (offset.y + dragOffsetY).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (abs(dragOffsetY) / 1000f).coerceIn(0f, 0.5f)
                                }
                                .transformable(state = transformState)
                        )
                    }

                    // Semi-transparent top bar overlay
                    AnimatedVisibility(
                        visible = showOverlay,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TopBarOverlay)
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = uiState.senderName.ifBlank { "Media" },
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (uiState.timestamp.isNotBlank()) {
                                    Text(
                                        text = uiState.timestamp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Share button
                            IconButton(
                                onClick = {
                                    val fileToShare = localFile
                                    if (fileToShare != null && fileToShare.exists()) {
                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                fileToShare
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = entity.mimeType
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, "Share via")
                                            )
                                        } catch (_: Exception) {
                                            // FileProvider not configured or file inaccessible
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
