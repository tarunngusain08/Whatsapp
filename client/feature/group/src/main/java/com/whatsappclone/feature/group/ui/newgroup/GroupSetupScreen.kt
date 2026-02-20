package com.whatsappclone.feature.group.ui.newgroup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.whatsappclone.core.common.util.UrlResolver
import com.whatsappclone.core.common.util.Constants
import com.whatsappclone.core.ui.components.LoadingOverlay
import com.whatsappclone.core.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSetupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChatDetail: (String) -> Unit,
    viewModel: NewGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onGroupAvatarSelected(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NewGroupEvent.NavigateToChatDetail -> onNavigateToChatDetail(event.chatId)
                is NewGroupEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "New Group",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
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
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = uiState.canCreateGroup,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    FloatingActionButton(
                        onClick = viewModel::onCreateGroup,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Create group"
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                GroupAvatarPlaceholder(
                    avatarUrl = uiState.groupAvatarUrl,
                    isUploading = uiState.isUploadingAvatar,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Group name field
                OutlinedTextField(
                    value = uiState.groupName,
                    onValueChange = viewModel::onGroupNameChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    label = { Text("Group name") },
                    placeholder = { Text("Enter group name") },
                    supportingText = {
                        Text(
                            text = "${uiState.groupName.length}/${Constants.MAX_GROUP_NAME_LENGTH}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.groupName.length > Constants.MAX_GROUP_NAME_LENGTH - 10) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Group description field (optional)
                OutlinedTextField(
                    value = uiState.groupDescription,
                    onValueChange = viewModel::onGroupDescriptionChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    label = { Text("Group description (optional)") },
                    placeholder = { Text("Add group description") },
                    supportingText = {
                        if (uiState.groupDescription.isNotEmpty()) {
                            Text(
                                text = "${uiState.groupDescription.length}/${Constants.MAX_GROUP_DESCRIPTION_LENGTH}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.groupDescription.length > Constants.MAX_GROUP_DESCRIPTION_LENGTH - 20) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Participants section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = "Participants",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Participants: ${uiState.selectedCount}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal list of selected participants
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = uiState.selectedContacts,
                        key = { it.userId }
                    ) { contact ->
                        ParticipantAvatarItem(contact = contact)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Loading overlay
        LoadingOverlay(isLoading = uiState.isLoading)
    }
}

@Composable
private fun GroupAvatarPlaceholder(
    avatarUrl: String?,
    isUploading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(128.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(enabled = !isUploading, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = UrlResolver.resolve(avatarUrl),
                    contentDescription = "Group icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Add group icon",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add icon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = !isUploading, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ParticipantAvatarItem(
    contact: ContactItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(
            url = contact.avatarUrl,
            name = contact.displayName,
            size = 48.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = contact.displayName.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}
