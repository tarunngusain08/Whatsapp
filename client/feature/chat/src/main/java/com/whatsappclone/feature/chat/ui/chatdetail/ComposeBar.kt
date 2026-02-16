package com.whatsappclone.feature.chat.ui.chatdetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.feature.chat.model.MessageUi

private val PlaceholderColor = Color(0xFF8696A0)
private val SendButtonGreen = Color(0xFF00A884)
private val AttachIconColor = Color(0xFF54656F)

@Composable
fun ComposeBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    replyToMessage: MessageUi? = null,
    onCancelReply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Reply preview — slides in above the compose bar
            ReplyPreview(
                replyMessage = replyToMessage,
                onCancelReply = onCancelReply
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Input field with attachment icon inside
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Attachment icon
                        IconButton(
                            onClick = onAttachmentClick,
                            modifier = Modifier
                                .padding(bottom = 2.dp)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = "Attach file",
                                tint = AttachIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Text input
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChanged,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp, max = 140.dp)
                                .padding(vertical = 12.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.None
                            ),
                            keyboardActions = KeyboardActions.Default,
                            maxLines = 6,
                            decorationBox = { innerTextField ->
                                if (text.isEmpty()) {
                                    Text(
                                        text = "Type a message",
                                        style = TextStyle(
                                            color = PlaceholderColor,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send / Mic button with animated transition
                AnimatedContent(
                    targetState = text.isNotBlank(),
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.7f,
                            animationSpec = tween(150)
                        ) + fadeIn(animationSpec = tween(150)))
                            .togetherWith(
                                scaleOut(
                                    targetScale = 0.7f,
                                    animationSpec = tween(150)
                                ) + fadeOut(animationSpec = tween(100))
                            )
                            .using(SizeTransform(clip = false))
                    },
                    contentAlignment = Alignment.Center,
                    label = "send_mic_transition"
                ) { hasText ->
                    FloatingActionButton(
                        onClick = {
                            if (hasText) onSendClick()
                        },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        containerColor = SendButtonGreen,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        if (hasText) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Record voice message",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
