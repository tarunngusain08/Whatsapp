package com.whatsappclone.feature.media.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.whatsappclone.core.common.util.UrlResolver
import kotlin.math.abs
import kotlin.math.roundToInt

private val TopBarOverlay = Color(0xCC000000)

/**
 * Fullscreen image viewer that loads from a URL directly.
 * Supports pinch-to-zoom, pan, double-tap zoom, swipe-down to dismiss,
 * and a tap-toggled top bar with back button and optional title.
 */
@Composable
fun ImageViewerScreen(
  imageUrl: String,
  title: String = "",
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current

  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
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
      .background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
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
          .data(UrlResolver.resolve(imageUrl))
          .crossfade(300)
          .build(),
        contentDescription = title.ifBlank { "Profile picture" },
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

        if (title.isNotBlank()) {
          Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .weight(1f)
              .padding(end = 16.dp)
          )
        }
      }
    }
  }
}
