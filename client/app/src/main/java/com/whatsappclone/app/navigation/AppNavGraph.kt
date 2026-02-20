package com.whatsappclone.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whatsappclone.app.lifecycle.WsLifecycleManager
import com.whatsappclone.core.network.url.BaseUrlProvider
import com.whatsappclone.feature.auth.data.AuthRepository
import com.whatsappclone.feature.chat.call.CallService
import com.whatsappclone.feature.chat.call.CallState
import com.whatsappclone.feature.auth.ui.login.LoginScreen
import com.whatsappclone.feature.auth.ui.otp.OtpScreen
import com.whatsappclone.feature.auth.ui.profile.ProfileSetupScreen
import com.whatsappclone.feature.auth.ui.splash.SplashScreen
import com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailScreen
import com.whatsappclone.feature.chat.ui.chatlist.ChatListScreen
import com.whatsappclone.feature.chat.ui.forward.ForwardPickerScreen
import com.whatsappclone.feature.contacts.ui.ContactInfoScreen
import com.whatsappclone.feature.contacts.ui.ContactPickerScreen
import com.whatsappclone.feature.group.ui.info.AddParticipantsScreen
import com.whatsappclone.feature.group.ui.info.GroupInfoScreen
import com.whatsappclone.feature.group.ui.newgroup.ContactSelectionScreen
import com.whatsappclone.feature.group.ui.newgroup.GroupSetupScreen
import com.whatsappclone.feature.group.ui.newgroup.NewGroupViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatsappclone.feature.media.ui.ImageViewerScreen
import com.whatsappclone.feature.media.ui.MediaViewerScreen
import com.whatsappclone.feature.profile.ui.ProfileEditScreen
import com.whatsappclone.feature.settings.ServerUrlScreen
import com.whatsappclone.feature.settings.ui.NotificationSettingsScreen
import com.whatsappclone.feature.settings.ui.PrivacySettingsScreen
import com.whatsappclone.feature.settings.ui.SettingsScreen
import com.whatsappclone.feature.settings.ui.ThemeSettingsScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sessionExpired: Boolean = false,
    onSessionExpiredHandled: () -> Unit = {},
    deepLinkChatId: String? = null,
    authRepository: AuthRepository,
    baseUrlProvider: BaseUrlProvider,
    wsLifecycleManager: WsLifecycleManager,
    callService: CallService? = null,
    isDebug: Boolean = false,
    onRestartApp: () -> Unit = {}
) {
    // Handle session expiry — navigate back to Login and clear the back stack
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            wsLifecycleManager.stop()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
            onSessionExpiredHandled()
        }
    }

    // Handle deep links to specific chats
    LaunchedEffect(deepLinkChatId) {
        deepLinkChatId?.let { chatId ->
            navController.navigate(AppRoute.ChatDetail.create(chatId))
        }
    }

    // Navigate to CallScreen when an incoming call arrives
    if (callService != null) {
        val incomingSession = callService.session.collectAsState().value
        LaunchedEffect(incomingSession?.callId) {
            val session = incomingSession ?: return@LaunchedEffect
            if (!session.isOutgoing && session.state == CallState.INCOMING_RINGING) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != AppRoute.CallScreen.route) {
                    navController.navigate(
                        AppRoute.CallScreen.create(
                            calleeName = session.remoteName,
                            avatarUrl = session.remoteAvatarUrl ?: "",
                            callType = session.callType,
                            calleeUserId = session.remoteUserId
                        )
                    )
                }
            }
        }
    }

    val navDuration = 350
    val navEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(navDuration, easing = navEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(navDuration, easing = navEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(navDuration, easing = navEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(navDuration, easing = navEasing)
            )
        }
    ) {

        // ── Splash ───────────────────────────────────────────────────────────
        composable(AppRoute.Splash.route) {
            SplashScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    wsLifecycleManager.startIfAuthenticated()
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(AppRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ────────────────────────────────────────────────────────────
        composable(AppRoute.Login.route) {
            LoginScreen(
                onNavigateToOtp = { phone, devOtp ->
                    navController.navigate(
                        AppRoute.OtpVerification.create(phone, devOtp)
                    )
                }
            )
        }

        // ── OTP Verification ─────────────────────────────────────────────────
        composable(
            route = AppRoute.OtpVerification.route,
            arguments = listOf(
                navArgument("phone") { type = NavType.StringType },
                navArgument("devOtp") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            OtpScreen(
                onNavigateToMain = {
                    wsLifecycleManager.startIfAuthenticated()
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(AppRoute.ProfileSetup.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Profile Setup ────────────────────────────────────────────────────
        composable(AppRoute.ProfileSetup.route) {
            ProfileSetupScreen(
                onNavigateToMain = {
                    wsLifecycleManager.startIfAuthenticated()
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Main (Chat List) ─────────────────────────────────────────────────
        composable(AppRoute.Main.route) {
            ChatListScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(AppRoute.ChatDetail.create(chatId))
                },
                onNavigateToContactPicker = {
                    navController.navigate(AppRoute.ContactPicker.route)
                },
                onNavigateToSettings = {
                    navController.navigate(AppRoute.Settings.route)
                },
                onNavigateToServerUrl = {
                    navController.navigate(AppRoute.ServerUrlSettings.route)
                },
                onNavigateToStarredMessages = {
                    navController.navigate(AppRoute.StarredMessages.route)
                },
                onNavigateToArchivedChats = {
                    navController.navigate(AppRoute.ArchivedChats.route)
                },
                onNavigateToStatus = {
                    navController.navigate(AppRoute.StatusList.route)
                }
            )
        }

        // ── Chat Detail ──────────────────────────────────────────────────────
        composable(
            route = AppRoute.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatDetailViewModel: com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailViewModel =
                hiltViewModel()

            val locationResult = backStackEntry.savedStateHandle
                .getStateFlow<String?>("location_result", null)
                .collectAsState()
            LaunchedEffect(locationResult.value) {
                locationResult.value?.let { value ->
                    backStackEntry.savedStateHandle.remove<String>("location_result")
                    val parts = value.split(",")
                    val lat = parts.getOrNull(0)?.toDoubleOrNull()
                    val lng = parts.getOrNull(1)?.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        chatDetailViewModel.sendLocationMessage(lat, lng)
                    }
                }
            }

            ChatDetailScreen(
                viewModel = chatDetailViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onViewContact = { userId ->
                    navController.navigate(AppRoute.ContactInfo.create(userId))
                },
                onNavigateToForward = { content, type ->
                    navController.navigate(
                        AppRoute.ForwardPicker.create(
                            messageId = "",
                            content = content ?: "",
                            type = type
                        )
                    )
                },
                onNavigateToReceiptDetails = { messageId ->
                    navController.navigate(AppRoute.ReceiptDetails.create(messageId))
                },
                onNavigateToWallpaper = { chatId ->
                    navController.navigate(AppRoute.Wallpaper.create(chatId))
                },
                onNavigateToLocationPicker = { chatId ->
                    navController.navigate(AppRoute.LocationPicker.create(chatId))
                }
            )
        }

        // ── Contact Picker ───────────────────────────────────────────────────
        composable(AppRoute.ContactPicker.route) {
            ContactPickerScreen(
                onNavigateToChatDetail = { chatId ->
                    navController.navigate(AppRoute.ChatDetail.create(chatId)) {
                        popUpTo(AppRoute.ContactPicker.route) { inclusive = true }
                    }
                },
                onNavigateToNewGroup = {
                    navController.navigate("group_creation_flow")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Contact Info ─────────────────────────────────────────────────────
        composable(
            route = AppRoute.ContactInfo.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) {
            ContactInfoScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(AppRoute.ChatDetail.create(chatId)) {
                        popUpTo(AppRoute.ContactInfo.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSharedMedia = { userId ->
                    navController.navigate(AppRoute.SharedMedia.create(userId))
                },
                onNavigateToCall = { userId, name, avatarUrl, callType ->
                    navController.navigate(
                        AppRoute.CallScreen.create(name, avatarUrl ?: "", callType, userId)
                    )
                }
            )
        }

        // ── Group Creation Flow (shared ViewModel) ─────────────────────────
        navigation(
            startDestination = AppRoute.NewGroup.route,
            route = "group_creation_flow"
        ) {
            composable(AppRoute.NewGroup.route) { backStackEntry ->
                val parentEntry = navController.getBackStackEntry("group_creation_flow")
                val sharedViewModel: NewGroupViewModel = hiltViewModel(parentEntry)
                ContactSelectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSetup = {
                        navController.navigate(AppRoute.GroupSetup.route)
                    },
                    viewModel = sharedViewModel
                )
            }

            composable(AppRoute.GroupSetup.route) { backStackEntry ->
                val parentEntry = navController.getBackStackEntry("group_creation_flow")
                val sharedViewModel: NewGroupViewModel = hiltViewModel(parentEntry)
                GroupSetupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChatDetail = { chatId ->
                        navController.navigate(AppRoute.ChatDetail.create(chatId)) {
                            popUpTo(AppRoute.Main.route)
                        }
                    },
                    viewModel = sharedViewModel
                )
            }
        }

        // ── Group Info ───────────────────────────────────────────────────────
        composable(
            route = AppRoute.GroupInfo.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) {
            GroupInfoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddParticipants = { chatId ->
                    navController.navigate(AppRoute.AddParticipants.create(chatId))
                },
                onNavigateToSharedMedia = { chatId ->
                    navController.navigate(AppRoute.SharedMedia.create(chatId))
                }
            )
        }

        // ── Add Participants ────────────────────────────────────────────────
        composable(
            route = AppRoute.AddParticipants.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) {
            AddParticipantsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Media Viewer ─────────────────────────────────────────────────────
        composable(
            route = AppRoute.MediaViewer.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("senderName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("timestamp") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            MediaViewerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Image Viewer (URL-based) ─────────────────────────────────────────
        composable(
            route = AppRoute.ImageViewer.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(navDuration, easing = navEasing)
                ) + fadeIn(tween(navDuration / 2))
            },
            exitTransition = { fadeOut(tween(navDuration / 2)) },
            popEnterTransition = { fadeIn(tween(navDuration / 2)) },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(navDuration, easing = navEasing)
                ) + fadeOut(tween(navDuration / 2))
            }
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            ImageViewerScreen(
                imageUrl = url,
                title = title,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Settings ─────────────────────────────────────────────────────────
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                onNavigateToProfileEdit = {
                    navController.navigate(AppRoute.ProfileEdit.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(AppRoute.NotificationSettings.route)
                },
                onNavigateToPrivacy = {
                    navController.navigate(AppRoute.PrivacySettings.route)
                },
                onNavigateToServerUrl = {
                    navController.navigate(AppRoute.ServerUrlSettings.route)
                },
                onNavigateToTheme = {
                    navController.navigate(AppRoute.ThemeSettings.route)
                },
                onNavigateToStorageUsage = {
                    navController.navigate(AppRoute.StorageUsage.route)
                },
                onNavigateToGlobalWallpaper = {
                    navController.navigate(AppRoute.Wallpaper.create("global"))
                },
                onNavigateToLogin = {
                    wsLifecycleManager.stop()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isDebug = isDebug
            )
        }

        // ── Theme Settings ──────────────────────────────────────────────────
        composable(AppRoute.ThemeSettings.route) {
            ThemeSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Storage Usage ────────────────────────────────────────────────────
        composable(AppRoute.StorageUsage.route) {
            com.whatsappclone.feature.settings.ui.StorageUsageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Profile Edit ─────────────────────────────────────────────────────
        composable(AppRoute.ProfileEdit.route) {
            ProfileEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToImageViewer = { url, title ->
                    navController.navigate(AppRoute.ImageViewer.create(url, title))
                }
            )
        }

        // ── Privacy Settings ─────────────────────────────────────────────────
        composable(AppRoute.PrivacySettings.route) {
            PrivacySettingsScreen(
                onNavigateToBlockedContacts = {
                    navController.navigate(AppRoute.BlockedContacts.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Wallpaper ─────────────────────────────────────────────────────────
        composable(
            route = AppRoute.Wallpaper.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            com.whatsappclone.feature.chat.ui.wallpaper.WallpaperScreen(
                chatId = chatId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Starred Messages ─────────────────────────────────────────────────
        composable(AppRoute.StarredMessages.route) {
            com.whatsappclone.feature.chat.ui.starred.StarredMessagesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(AppRoute.ChatDetail.create(chatId))
                }
            )
        }

        // ── Shared Media ─────────────────────────────────────────────────────
        composable(
            route = AppRoute.SharedMedia.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) {
            com.whatsappclone.feature.media.ui.SharedMediaScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Blocked Contacts ─────────────────────────────────────────────────
        composable(AppRoute.BlockedContacts.route) {
            com.whatsappclone.feature.contacts.ui.BlockedContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Notification Settings ────────────────────────────────────────────
        composable(AppRoute.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Server URL Settings (Debug Only) ─────────────────────────────────
        composable(AppRoute.ServerUrlSettings.route) {
            ServerUrlScreen(
                baseUrlProvider = baseUrlProvider,
                onRestartApp = onRestartApp
            )
        }

        // ── Archived Chats ──────────────────────────────────────────────────
        composable(AppRoute.ArchivedChats.route) {
            com.whatsappclone.feature.chat.ui.archived.ArchivedChatsScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(AppRoute.ChatDetail.create(chatId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Location Picker ──────────────────────────────────────────────────
        composable(
            route = AppRoute.LocationPicker.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            com.whatsappclone.feature.chat.ui.chatdetail.LocationPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onSendLocation = { lat, lng ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("location_result", "$lat,$lng")
                    navController.popBackStack()
                }
            )
        }

        // ── Status List ──────────────────────────────────────────────────────
        composable(AppRoute.StatusList.route) {
            com.whatsappclone.feature.chat.ui.status.StatusListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { userId, startIndex ->
                    navController.navigate(AppRoute.StatusViewer.create(userId, startIndex))
                },
                onNavigateToCreator = {
                    navController.navigate(AppRoute.StatusCreator.route)
                }
            )
        }

        // ── Status Viewer ───────────────────────────────────────────────────
        composable(
            route = AppRoute.StatusViewer.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("startIndex") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(navDuration, easing = navEasing)
                ) + fadeIn(tween(navDuration / 2))
            },
            exitTransition = { fadeOut(tween(navDuration / 2)) },
            popEnterTransition = { fadeIn(tween(navDuration / 2)) },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(navDuration, easing = navEasing)
                ) + fadeOut(tween(navDuration / 2))
            }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.whatsappclone.feature.chat.ui.status.StatusViewerScreen(
                userId = userId,
                startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Status Creator ──────────────────────────────────────────────────
        composable(
            AppRoute.StatusCreator.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(navDuration, easing = navEasing)
                )
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(navDuration, easing = navEasing)
                )
            }
        ) {
            val statusViewModel: com.whatsappclone.feature.chat.ui.status.StatusViewModel =
                hiltViewModel()
            com.whatsappclone.feature.chat.ui.status.StatusCreatorScreen(
                onNavigateBack = { navController.popBackStack() },
                onPost = { text, bgColor ->
                    statusViewModel.createTextStatus(text, bgColor)
                    navController.popBackStack()
                }
            )
        }

        // ── Receipt Details ──────────────────────────────────────────────────
        composable(
            route = AppRoute.ReceiptDetails.route,
            arguments = listOf(
                navArgument("messageId") { type = NavType.StringType }
            )
        ) {
            com.whatsappclone.feature.chat.ui.receipts.ReceiptDetailsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Call Screen ──────────────────────────────────────────────────────
        composable(
            route = AppRoute.CallScreen.route,
            arguments = listOf(
                navArgument("calleeName") { type = NavType.StringType },
                navArgument("avatarUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("callType") {
                    type = NavType.StringType
                    defaultValue = "audio"
                },
                navArgument("calleeUserId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(navDuration, easing = navEasing)
                )
            },
            exitTransition = { fadeOut(tween(navDuration / 2)) },
            popEnterTransition = { fadeIn(tween(navDuration / 2)) },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(navDuration, easing = navEasing)
                )
            }
        ) { backStackEntry ->
            val calleeName = backStackEntry.arguments?.getString("calleeName") ?: ""
            val avatarUrl = backStackEntry.arguments?.getString("avatarUrl")?.ifBlank { null }
            val callType = backStackEntry.arguments?.getString("callType") ?: "audio"
            com.whatsappclone.feature.chat.ui.call.CallScreen(
                calleeName = calleeName,
                calleeAvatarUrl = avatarUrl,
                callType = callType,
                onEndCall = { navController.popBackStack() }
            )
        }

        // ── Camera Screen ───────────────────────────────────────────────────
        composable(
            route = AppRoute.Camera.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(navDuration, easing = navEasing)
                )
            },
            exitTransition = { fadeOut(tween(navDuration / 2)) },
            popEnterTransition = { fadeIn(tween(navDuration / 2)) },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(navDuration, easing = navEasing)
                )
            }
        ) {
            com.whatsappclone.feature.media.ui.CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onImageCaptured = { navController.popBackStack() }
            )
        }

        // ── Forward Picker ───────────────────────────────────────────────────
        composable(
            route = AppRoute.ForwardPicker.route,
            arguments = listOf(
                navArgument("messageId") { type = NavType.StringType },
                navArgument("content") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "text"
                }
            )
        ) { backStackEntry ->
            val content = backStackEntry.arguments?.getString("content") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: "text"
            ForwardPickerScreen(
                messageContent = content,
                messageType = type,
                onNavigateBack = { navController.popBackStack() },
                onForwardComplete = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComingSoonScreen(
    title: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "This feature is under development",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
