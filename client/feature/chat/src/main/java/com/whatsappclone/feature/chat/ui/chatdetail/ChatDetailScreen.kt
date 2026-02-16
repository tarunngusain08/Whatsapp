package com.whatsappclone.feature.chat.ui.chatdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.whatsappclone.core.ui.components.DateSeparator
import com.whatsappclone.core.ui.components.TypingIndicator
import com.whatsappclone.core.ui.components.UserAvatar
import com.whatsappclone.core.ui.theme.WhatsAppColors
import com.whatsappclone.feature.chat.model.MessageUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Public entry point ──────────────────────────────────────────────────────

@Composable
fun ChatDetailScreen(
    onNavigateBack: () -> Unit,
    onViewContact: () -> Unit = {},
    onNavigateToForward: (messageContent: String?, messageType: String) -> Unit = { _, _ -> },
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val replyToMessage by viewModel.replyToMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.markAsRead()
    }

    ChatDetailContent(
        uiState = uiState,
        messages = messages,
        replyToMessage = replyToMessage,
        onNavigateBack = onNavigateBack,
        onViewContact = onViewContact,
        onComposerTextChanged = viewModel::onComposerTextChanged,
        onSendClick = viewModel::onSendMessage,
        onAttachmentClick = { /* TODO: attachment bottom sheet */ },
        onSetReply = viewModel::setReplyTo,
        onCancelReply = viewModel::clearReply,
        onToggleStar = viewModel::toggleStar,
        onDeleteForMe = viewModel::deleteForMe,
        onDeleteForEveryone = viewModel::deleteForEveryone,
        onCopyMessage = viewModel::copyToClipboard,
        onForwardMessage = onNavigateToForward,
        onErrorDismissed = viewModel::clearError
    )
}

// ── Screen content ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailContent(
    uiState: ChatDetailUiState,
    messages: LazyPagingItems<MessageUi>,
    replyToMessage: MessageUi?,
    onNavigateBack: () -> Unit,
    onViewContact: () -> Unit,
    onComposerTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onSetReply: (MessageUi?) -> Unit,
    onCancelReply: () -> Unit,
    onToggleStar: (String, Boolean) -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onForwardMessage: (String?, String) -> Unit,
    onErrorDismissed: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Message action sheet state
    var selectedMessage by remember { mutableStateOf<MessageUi?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<MessageUi?>(null) }

    // Highlighted message for "scroll to reply" animation
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    // Show errors via Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorDismissed()
        }
    }

    Scaffold(
        topBar = {
            ChatDetailTopBar(
                chatName = uiState.chatName,
                avatarUrl = uiState.chatAvatarUrl,
                subtitleText = uiState.subtitleText,
                isSubtitleHighlighted = uiState.isSubtitleHighlighted,
                onNavigateBack = onNavigateBack,
                onViewContact = onViewContact
            )
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(WhatsAppColors.ChatBackground)
            ) {
                MessageList(
                    messages = messages,
                    listState = listState,
                    isGroupChat = uiState.chatType == "group",
                    typingUsers = uiState.typingUsers,
                    highlightedMessageId = highlightedMessageId,
                    onMessageLongPress = { message ->
                        if (!message.isDeleted) {
                            selectedMessage = message
                        }
                    },
                    onQuotedReplyClick = { replyId ->
                        // Find the index of the original message and scroll to it
                        scope.launch {
                            val index = findMessageIndex(messages, replyId)
                            if (index >= 0) {
                                listState.animateScrollToItem(index)
                                highlightedMessageId = replyId
                                delay(1500)
                                highlightedMessageId = null
                            }
                        }
                    }
                )
            }

            // Compose bar (with reply preview integrated)
            ComposeBar(
                text = uiState.composerText,
                onTextChanged = onComposerTextChanged,
                onSendClick = onSendClick,
                onAttachmentClick = onAttachmentClick,
                replyToMessage = replyToMessage,
                onCancelReply = onCancelReply
            )
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
                    MessageAction.DELETE -> {
                        messageToDelete = msg
                        showDeleteDialog = true
                    }
                }
                selectedMessage = null
            },
            onDismiss = {
                selectedMessage = null
            }
        )
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
}

// ── Top App Bar ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailTopBar(
    chatName: String,
    avatarUrl: String?,
    subtitleText: String,
    isSubtitleHighlighted: Boolean,
    onNavigateBack: () -> Unit,
    onViewContact: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
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
                    text = { Text("Mute notifications") },
                    onClick = { menuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Search") },
                    onClick = { menuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Wallpaper") },
                    onClick = { menuExpanded = false }
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
    onMessageLongPress: (MessageUi) -> Unit,
    onQuotedReplyClick: (String) -> Unit
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

                Column {
                    // Message bubble with optional highlight
                    Box(
                        modifier = if (isHighlighted) {
                            Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0x3300A884),
                                        size = size
                                    )
                                }
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    ) {
                        MessageBubble(
                            message = message,
                            isGroupChat = isGroupChat,
                            onLongPress = onMessageLongPress,
                            onQuotedReplyClick = onQuotedReplyClick,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Date separator — shown after the message in reversed layout
                    // (appears above the message visually)
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
