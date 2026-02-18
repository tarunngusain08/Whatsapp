package com.whatsappclone.feature.contacts.ui

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatsappclone.core.database.relation.ContactWithUser
import com.whatsappclone.core.ui.components.EmptyState
import com.whatsappclone.core.ui.components.ErrorBanner
import com.whatsappclone.core.ui.components.LoadingOverlay
import com.whatsappclone.core.ui.components.UserAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    onNavigateToChatDetail: (String) -> Unit,
    onNavigateToNewGroup: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ContactPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
        }
    }

    // Track whether we've already asked for permission this session
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    // Request permission when screen opens if not already granted
    LaunchedEffect(uiState.hasContactPermission, uiState.isLoading) {
        if (!uiState.hasContactPermission && !hasRequestedPermission) {
            hasRequestedPermission = true
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ContactPickerNavigationEvent.NavigateToChatDetail ->
                    onNavigateToChatDetail(event.chatId)

                is ContactPickerNavigationEvent.NavigateToNewGroup ->
                    onNavigateToNewGroup()

                is ContactPickerNavigationEvent.NavigateBack ->
                    onNavigateBack()
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
                label = "contact_top_bar"
            ) { searching ->
                if (searching) {
                    ContactSearchTopBar(
                        query = uiState.searchQuery,
                        onQueryChanged = viewModel::onSearchQueryChanged,
                        onClose = {
                            isSearchActive = false
                            viewModel.onSearchQueryChanged("")
                        },
                        focusRequester = searchFocusRequester
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Select contact",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "${uiState.contacts.size} contacts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = viewModel::onBackClicked) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search contacts",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.hasContactPermission && !uiState.isLoading) {
                // Permission not granted state
                EmptyState(
                    icon = Icons.Filled.Contacts,
                    title = "Contacts permission required",
                    subtitle = "Allow access to your contacts to find friends on WhatsApp Clone"
                )
            } else if (uiState.contacts.isEmpty() && !uiState.isLoading && !uiState.isSyncing) {
                if (uiState.searchQuery.isNotBlank()) {
                    EmptyState(
                        icon = Icons.Filled.PersonSearch,
                        title = "No results found",
                        subtitle = "No contacts match \"${uiState.searchQuery}\""
                    )
                } else {
                    EmptyState(
                        icon = Icons.Filled.PersonSearch,
                        title = "No contacts found",
                        subtitle = "Contacts will appear here once other users register"
                    )
                }
            } else {
                ContactListWithSectionIndex(
                    uiState = uiState,
                    onContactClicked = viewModel::onContactClicked,
                    onNewGroupClicked = viewModel::onNewGroupClicked,
                    onSyncClicked = viewModel::syncContacts
                )
            }

            // Error banner
            ErrorBanner(
                message = uiState.error ?: "",
                isVisible = uiState.error != null,
                onRetry = viewModel::clearError,
                onDismiss = viewModel::clearError,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Loading overlay for chat creation
            LoadingOverlay(isLoading = uiState.isCreatingChat)
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun ContactListWithSectionIndex(
    uiState: ContactPickerUiState,
    onContactClicked: (ContactWithUser) -> Unit,
    onNewGroupClicked: () -> Unit,
    onSyncClicked: () -> Unit
) {
    val groupedContacts = uiState.groupedContacts
    val sectionLetters = uiState.sectionLetters
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Build a flat list with section headers for indexing
    val flatItems = remember(groupedContacts) {
        buildFlatItemList(groupedContacts)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // New Group option
            item(key = "new_group") {
                NewGroupRow(onClick = onNewGroupClicked)
            }

            // Refresh / Sync row
            item(key = "sync_row") {
                SyncRow(
                    isSyncing = uiState.isSyncing,
                    onClick = onSyncClicked
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Invite a friend row
            item(key = "invite_friend") {
                InviteFriendRow(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hey! I'm using WhatsApp Clone. Download it and chat with me!\nhttps://drive.google.com/drive/folders/1RNxcgI2TzRoa0MDKwCsbpl8aTj8i5nDR"
                        )
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Invite a friend")
                    )
                })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Contacts grouped by letter
            flatItems.forEach { item ->
                when (item) {
                    is FlatItem.SectionHeader -> {
                        item(key = "header_${item.letter}") {
                            SectionDivider(letter = item.letter)
                        }
                    }

                    is FlatItem.ContactItem -> {
                        item(key = "contact_${item.contact.contactId}") {
                            ContactRow(
                                contact = item.contact,
                                onClick = { onContactClicked(item.contact) }
                            )
                            if (!item.isLast) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section index on the right edge
        if (sectionLetters.size > 1) {
            SectionIndexBar(
                letters = sectionLetters,
                onLetterSelected = { letter ->
                    val headerKey = "header_$letter"
                    val targetIndex = 2 + flatItems.indexOfFirst {
                        it is FlatItem.SectionHeader && it.letter == letter
                    }
                    if (targetIndex >= 2) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
            )
        }
    }
}

@Composable
private fun SectionIndexBar(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var barHeightPx by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxHeight(0.7f)
            .onGloballyPositioned { coordinates ->
                barHeightPx = coordinates.size.height
            }
            .pointerInput(letters) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val y = change.position.y
                    val letterHeight = barHeightPx.toFloat() / letters.size
                    val index = (y / letterHeight).toInt().coerceIn(0, letters.size - 1)
                    onLetterSelected(letters[index])
                }
            }
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onLetterSelected(letter) }
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun SectionDivider(
    letter: Char,
    modifier: Modifier = Modifier
) {
    Text(
        text = letter.toString(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp, end = 16.dp)
    )
}

@Composable
private fun NewGroupRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.GroupAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "New group",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SyncRow(
    isSyncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isSyncing, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = if (isSyncing) "Syncing contacts..." else "Refresh contacts",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sync your phone contacts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactWithUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            UserAvatar(
                url = contact.avatarUrl,
                name = contact.deviceName,
                size = 48.dp
            )

            // Online indicator
            if (contact.isOnline == true) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 1.dp, y = 1.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .padding(2.dp)
                        .background(
                            color = Color(0xFF4CAF50),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.deviceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = contact.statusText ?: "Hey there! I am using WhatsApp Clone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InviteFriendRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Invite a friend",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSearchTopBar(
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

// ── Flat item model for building the contact list ──────────────────────────

private sealed class FlatItem {
    data class SectionHeader(val letter: Char) : FlatItem()
    data class ContactItem(val contact: ContactWithUser, val isLast: Boolean) : FlatItem()
}

private fun buildFlatItemList(
    groupedContacts: Map<Char, List<ContactWithUser>>
): List<FlatItem> {
    val items = mutableListOf<FlatItem>()
    groupedContacts.forEach { (letter, contacts) ->
        items.add(FlatItem.SectionHeader(letter))
        contacts.forEachIndexed { index, contact ->
            items.add(
                FlatItem.ContactItem(
                    contact = contact,
                    isLast = index == contacts.lastIndex
                )
            )
        }
    }
    return items
}
