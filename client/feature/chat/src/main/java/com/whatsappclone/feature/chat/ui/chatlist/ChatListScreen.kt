package com.whatsappclone.feature.chat.ui.chatlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatsappclone.core.ui.components.EmptyState
import com.whatsappclone.core.ui.components.ErrorBanner
import com.whatsappclone.core.ui.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToContactPicker: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToServerUrl: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ChatListNavigationEvent.NavigateToChat ->
                    onNavigateToChat(event.chatId)

                is ChatListNavigationEvent.NavigateToContactPicker ->
                    onNavigateToContactPicker()

                is ChatListNavigationEvent.NavigateToSettings ->
                    onNavigateToSettings()

                is ChatListNavigationEvent.NavigateToServerUrl ->
                    onNavigateToServerUrl()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "top_bar_transition"
            ) { searching ->
                if (searching) {
                    SearchTopBar(
                        query = uiState.searchQuery,
                        onQueryChanged = viewModel::onSearchQueryChanged,
                        onClose = {
                            isSearchActive = false
                            viewModel.onSearchQueryChanged("")
                        },
                        focusRequester = searchFocusRequester
                    )
                } else {
                    MainTopBar(
                        onSearchClicked = { isSearchActive = true },
                        onOverflowClicked = { showOverflowMenu = true },
                        showOverflowMenu = showOverflowMenu,
                        onDismissOverflow = { showOverflowMenu = false },
                        onSettingsClicked = {
                            showOverflowMenu = false
                            viewModel.onSettingsClicked()
                        },
                        onServerUrlClicked = {
                            showOverflowMenu = false
                            viewModel.onServerUrlClicked()
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onNewChatClicked,
                modifier = Modifier.padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = "New chat"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredChats = uiState.filteredChats

                if (filteredChats.isEmpty() && !uiState.isLoading && uiState.error == null) {
                    if (uiState.searchQuery.isNotBlank()) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "No results found",
                            subtitle = "No chats match \"${uiState.searchQuery}\""
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.Chat,
                            title = "No chats yet",
                            subtitle = "Start a new conversation by tapping the chat button below"
                        )
                    }
                } else {
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = filteredChats,
                            key = { it.chatId }
                        ) { chat ->
                            ChatItemRow(
                                chat = chat,
                                onClick = { viewModel.onChatClicked(chat.chatId) },
                                showDivider = chat != filteredChats.lastOrNull()
                            )
                        }
                    }
                }
            }

            ErrorBanner(
                message = uiState.error ?: "",
                isVisible = uiState.error != null,
                onRetry = viewModel::onRefresh,
                onDismiss = viewModel::clearError,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            LoadingOverlay(isLoading = uiState.isLoading && uiState.chats.isEmpty())
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    onSearchClicked: () -> Unit,
    onOverflowClicked: () -> Unit,
    showOverflowMenu: Boolean,
    onDismissOverflow: () -> Unit,
    onSettingsClicked: () -> Unit,
    onServerUrlClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "WhatsApp Clone",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        actions = {
            IconButton(onClick = onSearchClicked) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(onClick = { /* Camera placeholder */ }) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Box {
                IconButton(onClick = onOverflowClicked) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = onDismissOverflow,
                    modifier = Modifier.width(220.dp),
                    offset = DpOffset(x = 0.dp, y = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    OverflowMenuItem(
                        text = "New group",
                        onClick = onDismissOverflow
                    )
                    OverflowMenuItem(
                        text = "New broadcast",
                        onClick = onDismissOverflow
                    )
                    OverflowMenuItem(
                        text = "Linked devices",
                        onClick = onDismissOverflow
                    )
                    OverflowMenuItem(
                        text = "Starred messages",
                        onClick = onDismissOverflow
                    )
                    OverflowMenuItem(
                        text = "Settings",
                        onClick = onSettingsClicked
                    )
                    OverflowMenuItem(
                        text = "Server URL",
                        onClick = onServerUrlClicked
                    )
                }
            }
        }
    )
}

@Composable
private fun OverflowMenuItem(
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = onClick,
        modifier = Modifier.height(48.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        text = "Search...",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close search",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}
