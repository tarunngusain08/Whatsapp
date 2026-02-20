package com.whatsappclone.feature.chat.ui.chatdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.whatsappclone.core.ui.components.DateSeparator
import com.whatsappclone.core.ui.components.TypingIndicator
import com.whatsappclone.core.ui.components.UserAvatar
import com.whatsappclone.core.ui.theme.WhatsAppColors
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import com.whatsappclone.feature.chat.model.MessageUi
import com.whatsappclone.feature.media.audio.RecordingState
import com.whatsappclone.feature.media.audio.VoiceRecordingOverlay
import com.whatsappclone.feature.media.ui.AttachmentBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File

// ── Public entry point ──────────────────────────────────────────────────────

@Composable
fun ChatDetailScreen(
    onNavigateBack: () -> Unit,
    onViewContact: (userId: String) -> Unit = {},
    onNavigateToForward: (messageContent: String?, messageType: String) -> Unit = { _, _ -> },
    onNavigateToReceiptDetails: (messageId: String) -> Unit = {},
    onNavigateToWallpaper: (chatId: String) -> Unit = {},
    onNavigateToLocationPicker: (chatId: String) -> Unit = {},
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val replyToMessage by viewModel.replyToMessage.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.markAsRead()
    }

    ChatDetailContent(
        uiState = uiState,
        messages = messages,
        replyToMessage = replyToMessage,
        recordingState = recordingState,
        recordingAmplitudes = viewModel.recordingAmplitudes,
        onNavigateBack = onNavigateBack,
        onViewContact = { uiState.otherUserId?.let { onViewContact(it) } },
        onComposerTextChanged = viewModel::onComposerTextChanged,
        onSendClick = viewModel::onSendMessage,
        onAttachmentClick = {},
        onSendMediaMessage = viewModel::sendMediaMessage,
        onStartRecording = viewModel::startRecording,
        onStopRecording = viewModel::stopRecording,
        onCancelRecording = viewModel::cancelRecording,
        onLockRecording = viewModel::lockRecording,
        onSendRecording = viewModel::sendRecording,
        onSetReply = viewModel::setReplyTo,
        onCancelReply = viewModel::clearReply,
        onToggleStar = viewModel::toggleStar,
        onDeleteForMe = viewModel::deleteForMe,
        onDeleteForEveryone = viewModel::deleteForEveryone,
        onCopyMessage = viewModel::copyToClipboard,
        onForwardMessage = onNavigateToForward,
        onErrorDismissed = viewModel::clearError,
        onToggleMute = viewModel::toggleMute,
        onOpenSearch = viewModel::openSearch,
        onCloseSearch = viewModel::closeSearch,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onNextMatch = viewModel::navigateToNextMatch,
        onPrevMatch = viewModel::navigateToPreviousMatch,
        onSetDisappearingTimer = viewModel::setDisappearingMessages,
        onMessageInfo = onNavigateToReceiptDetails,
        onWallpaperClick = { onNavigateToWallpaper(uiState.chatId) },
        onLocationClick = { onNavigateToLocationPicker(uiState.chatId) },
        onExportChat = viewModel::exportChat,
        onScheduleMessage = viewModel::scheduleMessage,
        onEnterSelectionMode = viewModel::enterSelectionMode,
        onToggleMessageSelection = viewModel::toggleMessageSelection,
        onExitSelectionMode = viewModel::exitSelectionMode,
        onDeleteSelectedMessages = viewModel::deleteSelectedMessages,
        onStarSelectedMessages = viewModel::starSelectedMessages,
        onCopySelectedMessages = viewModel::copySelectedMessages,
        onReactionToggled = viewModel::toggleReaction
    )
}

// ── Screen content ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailContent(
    uiState: ChatDetailUiState,
    messages: LazyPagingItems<MessageUi>,
    replyToMessage: MessageUi?,
    recordingState: RecordingState,
    recordingAmplitudes: SharedFlow<Int>,
    onNavigateBack: () -> Unit,
    onViewContact: () -> Unit,
    onComposerTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onSendMediaMessage: (Uri, String, String?) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onLockRecording: () -> Unit,
    onSendRecording: () -> Unit,
    onSetReply: (MessageUi?) -> Unit,
    onCancelReply: () -> Unit,
    onToggleStar: (String, Boolean) -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onForwardMessage: (String?, String) -> Unit,
    onErrorDismissed: () -> Unit,
    onToggleMute: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onNextMatch: () -> Unit = {},
    onPrevMatch: () -> Unit = {},
    onSetDisappearingTimer: (String) -> Unit = {},
    onMessageInfo: (String) -> Unit = {},
    onWallpaperClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onExportChat: () -> Unit = {},
    onScheduleMessage: ((Long) -> Unit)? = null,
    onEnterSelectionMode: (String) -> Unit = {},
    onToggleMessageSelection: (String) -> Unit = {},
    onExitSelectionMode: () -> Unit = {},
    onDeleteSelectedMessages: () -> Unit = {},
    onStarSelectedMessages: () -> Unit = {},
    onCopySelectedMessages: () -> Unit = {},
    onImageClick: (MessageUi) -> Unit = {},
    onVideoClick: (MessageUi) -> Unit = {},
    onDocumentClick: (MessageUi) -> Unit = {},
    onDownloadClick: (MessageUi) -> Unit = {},
    onReactionToggled: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Message action sheet state
    var selectedMessage by remember { mutableStateOf<MessageUi?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<MessageUi?>(null) }
    var reactionPickerMessage by remember { mutableStateOf<MessageUi?>(null) }

    // Attachment bottom sheet state
    var showAttachmentSheet by remember { mutableStateOf(false) }

    // Camera capture URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Activity result launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            val mimeType = context.contentResolver.getType(selectedUri)
            val messageType = when {
                mimeType?.startsWith("video/") == true -> "video"
                else -> "image"
            }
            onSendMediaMessage(selectedUri, messageType, mimeType)
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            val mimeType = context.contentResolver.getType(selectedUri)
            onSendMediaMessage(selectedUri, "document", mimeType)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                onSendMediaMessage(uri, "image", "image/jpeg")
            }
        }
    }

    // Highlighted message for "scroll to reply" animation
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    // Auto-scroll to search match
    LaunchedEffect(uiState.currentMatchMessageId) {
        uiState.currentMatchMessageId?.let { matchId ->
            val index = findMessageIndex(messages, matchId)
            if (index >= 0) {
                listState.animateScrollToItem(index)
                highlightedMessageId = matchId
                delay(1500)
                highlightedMessageId = null
            }
        }
    }

    // Show errors via Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorDismissed()
        }
    }

    Scaffold(
        topBar = {
            when {
                uiState.isSelectionMode -> {
                    SelectionTopBar(
                        selectedCount = uiState.selectedCount,
                        onClose = onExitSelectionMode,
                        onDelete = onDeleteSelectedMessages,
                        onStar = onStarSelectedMessages,
                        onCopy = onCopySelectedMessages,
                        onForward = {
                            val firstSelectedMsg = (0 until messages.itemCount)
                                .mapNotNull { messages[it] }
                                .firstOrNull { it.messageId in uiState.selectedMessageIds }
                            onExitSelectionMode()
                            if (firstSelectedMsg != null) {
                                onForwardMessage(firstSelectedMsg.content, firstSelectedMsg.messageType)
                            }
                        }
                    )
                }
                uiState.isSearchActive -> {
                    SearchTopBar(
                        query = uiState.searchQuery,
                        onQueryChanged = onSearchQueryChanged,
                        matchCount = uiState.totalSearchMatches,
                        currentMatch = if (uiState.totalSearchMatches > 0) uiState.currentMatchIndex + 1 else 0,
                        onNextMatch = onNextMatch,
                        onPrevMatch = onPrevMatch,
                        onClose = onCloseSearch
                    )
                }
                else -> {
                    ChatDetailTopBar(
                        chatName = uiState.chatName,
                        avatarUrl = uiState.chatAvatarUrl,
                        subtitleText = uiState.subtitleText,
                        isSubtitleHighlighted = uiState.isSubtitleHighlighted,
                        isMuted = uiState.isMuted,
                        disappearingTimer = uiState.disappearingTimer,
                        onNavigateBack = onNavigateBack,
                        onViewContact = onViewContact,
                        onToggleMute = onToggleMute,
                        onOpenSearch = onOpenSearch,
                        onSetDisappearingTimer = onSetDisappearingTimer,
                        onWallpaperClick = onWallpaperClick,
                        onExportChat = onExportChat
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.ime
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Message list
            val wallpaperColorLong = remember(uiState.chatId) {
                com.whatsappclone.feature.chat.ui.wallpaper.getWallpaperColor(
                    context, uiState.chatId
                )
            }
            val chatBgColor = wallpaperColorLong?.let { Color(it) }
                ?: WhatsAppColors.ChatBackground

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(chatBgColor)
            ) {
                MessageList(
                    messages = messages,
                    listState = listState,
                    isGroupChat = uiState.chatType == "group",
                    typingUsers = uiState.typingUsers,
                    highlightedMessageId = highlightedMessageId,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedMessageIds = uiState.selectedMessageIds,
                    onMessageLongPress = { message ->
                        if (!message.isDeleted) {
                            if (uiState.isSelectionMode) {
                                onToggleMessageSelection(message.messageId)
                            } else {
                                selectedMessage = message
                            }
                        }
                    },
                    onMessageTap = { message ->
                        if (uiState.isSelectionMode) {
                            onToggleMessageSelection(message.messageId)
                        }
                    },
                    onQuotedReplyClick = { replyId ->
                        scope.launch {
                            val index = findMessageIndex(messages, replyId)
                            if (index >= 0) {
                                listState.animateScrollToItem(index)
                                highlightedMessageId = replyId
                                delay(1500)
                                highlightedMessageId = null
                            }
                        }
                    },
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onDocumentClick = onDocumentClick,
                    onDownloadClick = onDownloadClick,
                    onReactionToggled = onReactionToggled
                )
            }

            // Voice recording overlay replaces compose bar while recording
            if (recordingState.isRecording) {
                VoiceRecordingOverlay(
                    isVisible = true,
                    recordingState = recordingState,
                    amplitudes = recordingAmplitudes,
                    onCancel = onCancelRecording,
                    onStop = onStopRecording,
                    onLock = onLockRecording,
                    onSend = onSendRecording
                )
            } else {
                ComposeBar(
                    text = uiState.composerText,
                    onTextChanged = onComposerTextChanged,
                    onSendClick = onSendClick,
                    onAttachmentClick = { showAttachmentSheet = true },
                    onMicPressed = onStartRecording,
                    onScheduleMessage = onScheduleMessage,
                    replyToMessage = replyToMessage,
                    onCancelReply = onCancelReply
                )
            }
        }
    }

    // ── Bottom sheet for message actions ─────────────────────────────────

    if (selectedMessage != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        MessageActionSheet(
            message = selectedMessage!!,
            sheetState = sheetState,
            onAction = { action ->
                val msg = selectedMessage ?: return@MessageActionSheet
                when (action) {
                    MessageAction.REACT -> {
                        reactionPickerMessage = msg
                    }
                    MessageAction.REPLY -> {
                        onSetReply(msg)
                    }
                    MessageAction.FORWARD -> {
                        onForwardMessage(msg.content, msg.messageType)
                    }
                    MessageAction.COPY -> {
                        msg.content?.let { onCopyMessage(it) }
                    }
                    MessageAction.STAR -> {
                        onToggleStar(msg.messageId, msg.isStarred)
                    }
                    MessageAction.INFO -> {
                        onMessageInfo(msg.messageId)
                    }
                    MessageAction.DELETE -> {
                        messageToDelete = msg
                        showDeleteDialog = true
                    }
                    MessageAction.SELECT -> {
                        onEnterSelectionMode(msg.messageId)
                    }
                }
                selectedMessage = null
            },
            onDismiss = {
                selectedMessage = null
            }
        )
    }

    // ── Reaction picker ───────────────────────────────────────────────────

    if (reactionPickerMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ReactionPicker(
                visible = true,
                isOwnMessage = reactionPickerMessage!!.isOwnMessage,
                onReactionSelected = { emoji ->
                    onReactionToggled(reactionPickerMessage!!.messageId, emoji)
                    reactionPickerMessage = null
                },
                onExpandEmojiPicker = { reactionPickerMessage = null },
                onDismiss = { reactionPickerMessage = null }
            )
        }
    }

    // ── Delete confirmation dialog ──────────────────────────────────────

    if (showDeleteDialog && messageToDelete != null) {
        DeleteMessageDialog(
            message = messageToDelete!!,
            onDeleteForMe = {
                messageToDelete?.let { onDeleteForMe(it.messageId) }
                messageToDelete = null
            },
            onDeleteForEveryone = {
                messageToDelete?.let { onDeleteForEveryone(it.messageId) }
                messageToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                messageToDelete = null
            }
        )
    }

    // ── Attachment bottom sheet ──────────────────────────────────────

    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentSheet = false },
            onCameraClick = {
                val photoFile = File(
                    context.cacheDir,
                    "camera_${System.currentTimeMillis()}.jpg"
                )
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            },
            onGalleryClick = {
                galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    )
                )
            },
            onDocumentClick = {
                documentLauncher.launch(arrayOf("*/*"))
            },
            onLocationClick = {
                onLocationClick()
            }
        )
    }
}

// ── Top App Bar ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailTopBar(
    chatName: String,
    avatarUrl: String?,
    subtitleText: String,
    isSubtitleHighlighted: Boolean,
    isMuted: Boolean = false,
    disappearingTimer: String = "off",
    onNavigateBack: () -> Unit,
    onViewContact: () -> Unit,
    onToggleMute: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onSetDisappearingTimer: (String) -> Unit = {},
    onWallpaperClick: () -> Unit = {},
    onExportChat: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDisappearingSheet by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onViewContact() }
            ) {
                UserAvatar(
                    url = avatarUrl,
                    name = chatName,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chatName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitleText.isNotEmpty()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSubtitleHighlighted) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.75f)
                            },
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("View contact") },
                    onClick = {
                        menuExpanded = false
                        onViewContact()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications") },
                    onClick = {
                        menuExpanded = false
                        onToggleMute()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Search") },
                    onClick = {
                        menuExpanded = false
                        onOpenSearch()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Disappearing messages") },
                    onClick = {
                        menuExpanded = false
                        showDisappearingSheet = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Wallpaper") },
                    onClick = {
                        menuExpanded = false
                        onWallpaperClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export chat") },
                    onClick = {
                        menuExpanded = false
                        onExportChat()
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )

    if (showDisappearingSheet) {
        DisappearingMessagesSheet(
            currentTimer = disappearingTimer,
            onTimerSelected = { timer ->
                onSetDisappearingTimer(timer)
                showDisappearingSheet = false
            },
            onDismiss = { showDisappearingSheet = false }
        )
    }
}

// ── Disappearing Messages Bottom Sheet ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisappearingMessagesSheet(
    currentTimer: String,
    onTimerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "off" to "Off",
        "24h" to "24 hours",
        "7d" to "7 days",
        "90d" to "90 days"
    )

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Disappearing messages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            Text(
                text = "New messages will disappear from this chat after the selected duration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.size(8.dp))

            options.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTimerSelected(value) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = currentTimer == value,
                        onClick = { onTimerSelected(value) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// ── Search Top Bar ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    matchCount: Int,
    currentMatch: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            androidx.compose.material3.TextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        "Search messages...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                },
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close search",
                    tint = Color.White
                )
            }
        },
        actions = {
            if (matchCount > 0) {
                Text(
                    text = "$currentMatch/$matchCount",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            IconButton(onClick = onPrevMatch, enabled = matchCount > 0) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Previous match",
                    tint = if (matchCount > 0) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onNextMatch, enabled = matchCount > 0) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Next match",
                    tint = if (matchCount > 0) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = 180f }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

// ── Selection Top Bar ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onStar: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Exit selection",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onStar) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Star selected",
                    tint = Color.White
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete selected",
                    tint = Color.White
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy selected",
                    tint = Color.White
                )
            }
            IconButton(onClick = onForward) {
                Icon(
                    imageVector = Icons.Filled.Forward,
                    contentDescription = "Forward selected",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

// ── Message list ────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages: LazyPagingItems<MessageUi>,
    listState: LazyListState,
    isGroupChat: Boolean,
    typingUsers: Set<String>,
    highlightedMessageId: String?,
    isSelectionMode: Boolean = false,
    selectedMessageIds: Set<String> = emptySet(),
    onMessageLongPress: (MessageUi) -> Unit,
    onMessageTap: (MessageUi) -> Unit = {},
    onQuotedReplyClick: (String) -> Unit,
    onImageClick: (MessageUi) -> Unit = {},
    onVideoClick: (MessageUi) -> Unit = {},
    onDocumentClick: (MessageUi) -> Unit = {},
    onDownloadClick: (MessageUi) -> Unit = {},
    onReactionToggled: (String, String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()

    // Determine whether user has scrolled up from the bottom
    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 3
        }
    }

    // Auto-scroll to bottom when a new message arrives and user is near bottom
    LaunchedEffect(messages.itemCount) {
        if (listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    val highlightColor = WhatsAppColors.MessageHighlight
    val selectionColor = WhatsAppColors.SelectionHighlight

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 8.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // Typing indicator at the very bottom (index 0 in reversed layout)
            if (typingUsers.isNotEmpty()) {
                item(key = "typing_indicator") {
                    TypingIndicator(
                        names = typingUsers.toList(),
                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                    )
                }
            }

            // Messages
            items(
                count = messages.itemCount,
                key = { index -> messages[index]?.messageId ?: "msg_$index" }
            ) { index ->
                val message = messages[index] ?: return@items
                val isHighlighted = message.messageId == highlightedMessageId
                val isSelected = message.messageId in selectedMessageIds

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isHighlighted) {
                                    Modifier.drawBehind {
                                        drawRect(
                                            color = highlightColor,
                                            size = size
                                        )
                                    }
                                } else if (isSelected) {
                                    Modifier.drawBehind {
                                        drawRect(
                                            color = selectionColor,
                                            size = size
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (isSelectionMode) {
                                    Modifier.clickable { onMessageTap(message) }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onMessageTap(message) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            when (message.messageType) {
                                "system" -> {
                                    SystemMessageItem(
                                        text = message.content ?: "",
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                                "image", "video", "audio", "document" -> {
                                    MediaMessageBubble(
                                        message = message,
                                        uploadProgress = null,
                                        isDownloaded = false,
                                        onImageClick = onImageClick,
                                        onVideoClick = onVideoClick,
                                        onDocumentClick = onDocumentClick,
                                        onDownloadClick = onDownloadClick,
                                        onLongPress = onMessageLongPress,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                                else -> {
                                    MessageBubble(
                                        message = message,
                                        isGroupChat = isGroupChat,
                                        onLongPress = onMessageLongPress,
                                        onQuotedReplyClick = onQuotedReplyClick,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (message.reactions.isNotEmpty()) {
                        ReactionChips(
                            reactions = message.reactions,
                            isOwnMessage = message.isOwnMessage,
                            onReactionToggled = { emoji ->
                                onReactionToggled(message.messageId, emoji)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    if (message.showDateSeparator && message.dateSeparatorText != null) {
                        DateSeparator(text = message.dateSeparatorText)
                    }
                }
            }

            // Loading indicator for paging
            when (messages.loadState.append) {
                is LoadState.Loading -> {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                else -> Unit
            }
        }

        // Scroll-to-bottom FAB
        AnimatedVisibility(
            visible = showScrollToBottom,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            enter = scaleIn(
                initialScale = 0.6f,
                animationSpec = tween(200)
            ) + fadeIn(animationSpec = tween(200)),
            exit = scaleOut(
                targetScale = 0.6f,
                animationSpec = tween(150)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun findMessageIndex(messages: LazyPagingItems<MessageUi>, messageId: String): Int {
    for (i in 0 until messages.itemCount) {
        if (messages.peek(i)?.messageId == messageId) return i
    }
    return -1
}
