package com.whatsappclone.feature.chat.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsappclone.core.ui.components.UserAvatar

private val SectionHeaderColor = Color(0xFF00A884)
private val SnippetTextColor = Color(0xFF667781)
private val HighlightColor = Color(0xFF00A884)
private val DividerColor = Color(0xFFE9EDEF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (chatId: String) -> Unit,
    onNavigateToMessage: (chatId: String, messageId: String) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GlobalSearchEvent.NavigateToChat -> onNavigateToChat(event.chatId)
                is GlobalSearchEvent.NavigateToMessage -> onNavigateToMessage(event.chatId, event.messageId)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            SearchTopBar(
                query = uiState.query,
                onQueryChanged = viewModel::onQueryChanged,
                onClearQuery = { viewModel.onQueryChanged("") },
                onNavigateBack = {
                    viewModel.clearSearch()
                    onNavigateBack()
                },
                focusRequester = focusRequester,
                onSearch = { keyboardController?.hide() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isSearching -> {
                    LoadingIndicator()
                }

                uiState.showEmptyState && uiState.query.length >= 2 -> {
                    EmptySearchState(query = uiState.query)
                }

                uiState.hasResults -> {
                    SearchResultsList(
                        contactResults = uiState.contactResults,
                        messageResults = uiState.messageResults,
                        query = uiState.query,
                        onContactClicked = viewModel::onContactClicked,
                        onMessageClicked = viewModel::onMessageClicked
                    )
                }

                uiState.query.isEmpty() -> {
                    SearchPrompt()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onNavigateBack: () -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit
) {
    TopAppBar(
        title = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search...",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        IconButton(
                            onClick = onClearQuery,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun SearchResultsList(
    contactResults: List<ContactSearchResult>,
    messageResults: List<MessageSearchResult>,
    query: String,
    onContactClicked: (ContactSearchResult) -> Unit,
    onMessageClicked: (MessageSearchResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Contacts section
        if (contactResults.isNotEmpty()) {
            item(key = "contacts_header") {
                SectionHeader(title = "Chats")
            }

            items(
                items = contactResults,
                key = { it.userId }
            ) { contact ->
                ContactResultItem(
                    result = contact,
                    onClick = { onContactClicked(contact) }
                )
            }
        }

        // Messages section
        if (messageResults.isNotEmpty()) {
            item(key = "messages_header") {
                SectionHeader(title = "Messages")
            }

            items(
                items = messageResults,
                key = { it.messageId }
            ) { message ->
                MessageResultItem(
                    result = message,
                    query = query,
                    onClick = { onMessageClicked(message) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = SectionHeaderColor,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 16.dp)
    )
}

@Composable
private fun ContactResultItem(
    result: ContactSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            url = result.avatarUrl,
            name = result.displayName,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (result.statusText != null) {
                Text(
                    text = result.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = SnippetTextColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}

@Composable
private fun MessageResultItem(
    result: MessageSearchResult,
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chat avatar
        UserAvatar(
            url = result.chatAvatarUrl,
            name = result.chatName,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Chat name + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.chatName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = result.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = SnippetTextColor,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Message snippet with highlighted match
            HighlightedSnippet(
                text = result.content,
                query = query,
                highlightRange = result.matchHighlightRange
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}

@Composable
private fun HighlightedSnippet(
    text: String,
    query: String,
    highlightRange: IntRange?
) {
    val annotatedString = remember(text, query, highlightRange) {
        buildAnnotatedString {
            if (highlightRange != null && highlightRange.first >= 0 && highlightRange.last < text.length) {
                // Show context around the match
                val contextStart = (highlightRange.first - 30).coerceAtLeast(0)
                val contextEnd = (highlightRange.last + 40).coerceAtMost(text.length)

                val prefix = if (contextStart > 0) "..." else ""
                val suffix = if (contextEnd < text.length) "..." else ""

                val snippet = text.substring(contextStart, contextEnd)
                val highlightStartInSnippet = highlightRange.first - contextStart
                val highlightEndInSnippet = highlightRange.last + 1 - contextStart

                withStyle(SpanStyle(color = SnippetTextColor)) {
                    append(prefix)
                }

                if (highlightStartInSnippet > 0) {
                    withStyle(SpanStyle(color = SnippetTextColor)) {
                        append(snippet.substring(0, highlightStartInSnippet))
                    }
                }

                withStyle(
                    SpanStyle(
                        color = HighlightColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(
                        snippet.substring(
                            highlightStartInSnippet,
                            highlightEndInSnippet.coerceAtMost(snippet.length)
                        )
                    )
                }

                if (highlightEndInSnippet < snippet.length) {
                    withStyle(SpanStyle(color = SnippetTextColor)) {
                        append(snippet.substring(highlightEndInSnippet))
                    }
                }

                withStyle(SpanStyle(color = SnippetTextColor)) {
                    append(suffix)
                }
            } else {
                withStyle(SpanStyle(color = SnippetTextColor)) {
                    append(text.take(80))
                    if (text.length > 80) append("...")
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 14.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 18.sp
    )
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No chats or messages match \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SearchPrompt() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Search chats and messages",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}
