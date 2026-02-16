package com.whatsappclone.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is NotificationSettingsEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Message notifications ────────────────────────────────────
            NotificationSectionLabel(text = "Message notifications")

            NotificationToggleItem(
                title = "Notification sounds",
                subtitle = "Play sounds for incoming messages",
                isEnabled = prefs.messageNotificationsEnabled,
                onToggle = viewModel::onMessageNotificationsToggled
            )

            NotificationPickerItem(
                title = "Notification tone",
                currentValue = prefs.messageTone,
                options = listOf("Default", "None", "Tone 1", "Tone 2", "Tone 3"),
                onValueChanged = viewModel::onMessageToneChanged
            )

            NotificationPickerItem(
                title = "Vibrate",
                currentValue = prefs.messageVibrate,
                options = listOf("Default", "Off", "Short", "Long"),
                onValueChanged = viewModel::onMessageVibrateChanged
            )

            NotificationSettingsDivider()

            // ── Group notifications ──────────────────────────────────────
            NotificationSectionLabel(text = "Group notifications")

            NotificationToggleItem(
                title = "Notification sounds",
                subtitle = "Play sounds for group messages",
                isEnabled = prefs.groupNotificationsEnabled,
                onToggle = viewModel::onGroupNotificationsToggled
            )

            NotificationPickerItem(
                title = "Notification tone",
                currentValue = prefs.groupTone,
                options = listOf("Default", "None", "Tone 1", "Tone 2", "Tone 3"),
                onValueChanged = viewModel::onGroupToneChanged
            )

            NotificationPickerItem(
                title = "Vibrate",
                currentValue = prefs.groupVibrate,
                options = listOf("Default", "Off", "Short", "Long"),
                onValueChanged = viewModel::onGroupVibrateChanged
            )

            NotificationSettingsDivider()

            // ── In-app notifications ─────────────────────────────────────
            NotificationSectionLabel(text = "In-app notifications")

            NotificationToggleItem(
                title = "In-app notification sounds",
                subtitle = "Play sounds while the app is open",
                isEnabled = prefs.inAppSounds,
                onToggle = viewModel::onInAppSoundsToggled
            )

            NotificationToggleItem(
                title = "In-app notification preview",
                subtitle = "Show notification preview at the top of the screen",
                isEnabled = prefs.inAppPreview,
                onToggle = viewModel::onInAppPreviewToggled
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun NotificationSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        fontSize = 13.sp
    )
}

@Composable
private fun NotificationToggleItem(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun NotificationPickerItem(
    title: String,
    currentValue: String,
    options: List<String>,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        PickerDialog(
            title = title,
            currentValue = currentValue,
            options = options,
            onValueSelected = { value ->
                onValueChanged(value)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PickerDialog(
    title: String,
    currentValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueSelected(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == option,
                            onClick = { onValueSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NotificationSettingsDivider(modifier: Modifier = Modifier) {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
    Spacer(modifier = Modifier.height(8.dp))
}
