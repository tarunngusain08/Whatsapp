package com.whatsappclone.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatsappclone.core.ui.components.LoadingOverlay
import com.whatsappclone.core.ui.components.UserAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToServerUrl: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    isDebug: Boolean = false,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showComingSoon: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar("Coming soon") }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is SettingsNavigationEvent.NavigateToProfileEdit -> onNavigateToProfileEdit()
                is SettingsNavigationEvent.NavigateToAccount -> { /* placeholder */ }
                is SettingsNavigationEvent.NavigateToNotifications -> onNavigateToNotifications()
                is SettingsNavigationEvent.NavigateToPrivacy -> onNavigateToPrivacy()
                is SettingsNavigationEvent.NavigateToServerUrl -> onNavigateToServerUrl()
                is SettingsNavigationEvent.NavigateToLogin -> onNavigateToLogin()
                is SettingsNavigationEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    if (uiState.showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = viewModel::onLogoutConfirmed,
            onDismiss = viewModel::onLogoutDismissed
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Profile card ─────────────────────────────────────────
                ProfileCard(
                    displayName = uiState.currentUser?.displayName ?: "User",
                    statusText = uiState.currentUser?.statusText
                        ?: "Hey there! I am using WhatsApp Clone",
                    avatarUrl = uiState.currentUser?.avatarUrl,
                    onClick = viewModel::onProfileClicked
                )

                SettingsSectionDivider()

                // ── Account section ──────────────────────────────────────
                SectionHeader(text = "Account")

                SettingsItem(
                    icon = Icons.Filled.Lock,
                    iconTint = Color(0xFF00BFA5),
                    title = "Privacy",
                    subtitle = "Block contacts, disappearing messages",
                    onClick = viewModel::onPrivacyClicked
                )

                SettingsItem(
                    icon = Icons.Filled.Security,
                    iconTint = Color(0xFF00BFA5),
                    title = "Two-step verification",
                    subtitle = "Enable two-step verification for security",
                    onClick = showComingSoon
                )

                SettingsItem(
                    icon = Icons.Filled.PhoneAndroid,
                    iconTint = Color(0xFF00BFA5),
                    title = "Change number",
                    subtitle = "Change your phone number",
                    onClick = showComingSoon
                )

                SettingsItem(
                    icon = Icons.Filled.Key,
                    iconTint = Color(0xFF00BFA5),
                    title = "Request account info",
                    subtitle = "Request your account information",
                    onClick = showComingSoon
                )

                SettingsSectionDivider()

                // ── Chats section ────────────────────────────────────────
                SectionHeader(text = "Chats")

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconTint = Color(0xFF25D366),
                    title = "Theme",
                    subtitle = "System default",
                    onClick = showComingSoon
                )

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconTint = Color(0xFF25D366),
                    title = "Wallpaper",
                    subtitle = "Change chat wallpaper",
                    onClick = showComingSoon
                )

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconTint = Color(0xFF25D366),
                    title = "Chat backup",
                    subtitle = "Back up your chat history",
                    onClick = showComingSoon
                )

                SettingsSectionDivider()

                // ── Notifications section ────────────────────────────────
                SectionHeader(text = "Notifications")

                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    iconTint = Color(0xFFFF6D00),
                    title = "Notifications",
                    subtitle = "Message, group & call tones",
                    onClick = viewModel::onNotificationsClicked
                )

                SettingsSectionDivider()

                // ── Storage and data section ─────────────────────────────
                SectionHeader(text = "Storage and data")

                SettingsItem(
                    icon = Icons.Filled.DataUsage,
                    iconTint = Color(0xFF7C4DFF),
                    title = "Storage usage",
                    subtitle = "Manage your storage",
                    onClick = showComingSoon
                )

                SettingsItem(
                    icon = Icons.Filled.DataUsage,
                    iconTint = Color(0xFF7C4DFF),
                    title = "Network usage",
                    subtitle = "View network usage statistics",
                    onClick = showComingSoon
                )

                SettingsItem(
                    icon = Icons.Filled.DataUsage,
                    iconTint = Color(0xFF7C4DFF),
                    title = "Auto-download media",
                    subtitle = "Configure when to download media",
                    onClick = showComingSoon
                )

                SettingsSectionDivider()

                // ── Help section ─────────────────────────────────────────
                SectionHeader(text = "Help")

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconTint = Color(0xFF039BE5),
                    title = "Help center",
                    subtitle = "Get help, contact us, privacy policy",
                    onClick = showComingSoon
                )

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconTint = Color(0xFF039BE5),
                    title = "Terms and Privacy Policy",
                    subtitle = "Read our terms and privacy policy",
                    onClick = showComingSoon
                )

                // Debug-only server URL option
                if (isDebug) {
                    SettingsSectionDivider()

                    SectionHeader(text = "Developer")

                    SettingsItem(
                        icon = Icons.Filled.Cloud,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Server URL",
                        subtitle = "Configure backend server address",
                        onClick = viewModel::onServerUrlClicked
                    )
                }

                SettingsSectionDivider()

                // ── Logout ───────────────────────────────────────────────
                LogoutButton(onClick = viewModel::onLogoutClicked)

                Spacer(modifier = Modifier.height(16.dp))

                // App version footer
                Text(
                    text = "WhatsApp Clone v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            LoadingOverlay(isLoading = uiState.isLoggingOut)
        }
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun ProfileCard(
    displayName: String,
    statusText: String,
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            url = avatarUrl,
            name = displayName,
            size = 72.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 64.dp, top = 16.dp, bottom = 8.dp),
        fontSize = 13.sp
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsSectionDivider(modifier: Modifier = Modifier) {
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Log out",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log out?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to log out? You will need to verify your phone number again to log back in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Log out",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
