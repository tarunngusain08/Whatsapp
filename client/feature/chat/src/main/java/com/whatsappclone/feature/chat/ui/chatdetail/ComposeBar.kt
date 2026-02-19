package com.whatsappclone.feature.chat.ui.chatdetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsappclone.feature.chat.model.MessageUi

private val PlaceholderColor = Color(0xFF8696A0)
private val SendButtonGreen = Color(0xFF00A884)
private val AttachIconColor = Color(0xFF54656F)
private val EmojiIconColor = Color(0xFF54656F)

private val CommonEmojis = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE05", "\uD83D\uDE06", "\uD83D\uDE09",
    "\uD83D\uDE0A", "\uD83D\uDE0D", "\uD83D\uDE18", "\uD83D\uDE1C", "\uD83D\uDE1D",
    "\uD83E\uDD23", "\uD83E\uDD70", "\uD83E\uDD72", "\uD83E\uDD29", "\uD83E\uDD2D",
    "\uD83D\uDE0E", "\uD83D\uDE14", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE31",
    "\uD83D\uDE33", "\uD83D\uDE44", "\uD83D\uDE4F", "\uD83D\uDE21", "\uD83E\uDD14",
    "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4B", "\uD83D\uDC4F", "\uD83D\uDE4C",
    "\u270C\uFE0F", "\uD83E\uDD1E", "\uD83E\uDD1F", "\uD83D\uDCAA", "\uD83E\uDD1D",
    "\u270B", "\uD83D\uDC48", "\uD83D\uDC49", "\uD83D\uDC46", "\uD83D\uDC47",
    "\u2764\uFE0F", "\uD83E\uDDE1", "\uD83D\uDC9B", "\uD83D\uDC9A", "\uD83D\uDC99",
    "\uD83D\uDC9C", "\uD83D\uDDA4", "\uD83E\uDD0D", "\uD83D\uDC94", "\u2763\uFE0F",
    "\uD83D\uDD25", "\u2728", "\uD83C\uDF89", "\uD83C\uDF8A", "\uD83C\uDF1F",
    "\uD83D\uDCA5", "\uD83D\uDCAF", "\u2705", "\u274C", "\u2757",
    "\u2753", "\uD83D\uDCAC", "\uD83D\uDC40", "\uD83D\uDCA4", "\uD83C\uDFB5",
    "\uD83C\uDFB6", "\uD83D\uDCF7", "\uD83C\uDF3A", "\uD83C\uDF39", "\uD83C\uDF3B",
    "\u2600\uFE0F", "\uD83C\uDF19", "\u26A1", "\uD83C\uDF08", "\u2744\uFE0F",
    "\uD83C\uDF7A", "\u2615", "\uD83C\uDF55", "\uD83C\uDF82", "\uD83C\uDF70"
)

@Composable
fun ComposeBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onMicPressed: () -> Unit = {},
    onMicReleased: () -> Unit = {},
    onScheduleMessage: ((Long) -> Unit)? = null,
    replyToMessage: MessageUi? = null,
    onCancelReply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSchedulePicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }

    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length))
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 0.dp, end = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(
                            onClick = { showEmojiPicker = !showEmojiPicker },
                            modifier = Modifier
                                .padding(bottom = 2.dp)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (showEmojiPicker) Icons.Filled.Keyboard else Icons.Filled.EmojiEmotions,
                                contentDescription = if (showEmojiPicker) "Show keyboard" else "Show emojis",
                                tint = EmojiIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                onTextChanged(newValue.text)
                            },
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
                                if (textFieldValue.text.isEmpty()) {
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
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

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
                        modifier = Modifier
                            .size(48.dp)
                            .then(
                                if (!hasText) {
                                    Modifier.pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown()
                                            onMicPressed()
                                            waitForUpOrCancellation()
                                            onMicReleased()
                                        }
                                    }
                                } else if (onScheduleMessage != null) {
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                showSchedulePicker = true
                                            },
                                            onTap = {
                                                onSendClick()
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
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
                                contentDescription = "Send message (long-press to schedule)",
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

            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                EmojiPickerGrid(
                    onEmojiSelected = { emoji ->
                        val cursorPos = textFieldValue.selection.start
                        val newText = textFieldValue.text.substring(0, cursorPos) +
                                emoji +
                                textFieldValue.text.substring(cursorPos)
                        val newCursor = cursorPos + emoji.length
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                        onTextChanged(newText)
                    }
                )
            }
        }
    }

    if (showSchedulePicker && onScheduleMessage != null) {
        ScheduleMessageDialog(
            onSchedule = { millis ->
                onScheduleMessage(millis)
                showSchedulePicker = false
            },
            onDismiss = { showSchedulePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPickerGrid(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CommonEmojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleMessageDialog(
    onSchedule: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scheduleOptions = listOf(
        "In 15 minutes" to 15L * 60 * 1000,
        "In 1 hour" to 60L * 60 * 1000,
        "In 3 hours" to 3L * 60 * 60 * 1000,
        "Tomorrow morning (9 AM)" to calculateTomorrowMorning()
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule message",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose when to send this message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                scheduleOptions.forEach { (label, offsetMs) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val scheduledTime = System.currentTimeMillis() + offsetMs
                                onSchedule(scheduledTime)
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun calculateTomorrowMorning(): Long {
    val now = java.util.Calendar.getInstance()
    val tomorrow = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 9)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return tomorrow.timeInMillis - now.timeInMillis
}
