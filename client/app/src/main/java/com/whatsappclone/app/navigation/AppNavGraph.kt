package com.whatsappclone.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whatsappclone.app.lifecycle.WsLifecycleManager
import com.whatsappclone.core.network.url.BaseUrlProvider
import com.whatsappclone.feature.auth.data.AuthRepository
import com.whatsappclone.feature.auth.ui.login.LoginScreen
import com.whatsappclone.feature.auth.ui.otp.OtpScreen
import com.whatsappclone.feature.auth.ui.profile.ProfileSetupScreen
import com.whatsappclone.feature.auth.ui.splash.SplashScreen
import com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailScreen
import com.whatsappclone.feature.chat.ui.chatlist.ChatListScreen
import com.whatsappclone.feature.contacts.ui.ContactPickerScreen
import com.whatsappclone.feature.group.ui.info.AddParticipantsScreen
import com.whatsappclone.feature.group.ui.info.GroupInfoScreen
import com.whatsappclone.feature.media.ui.MediaViewerScreen
import com.whatsappclone.feature.profile.ui.ProfileEditScreen
import com.whatsappclone.feature.settings.ServerUrlScreen
import com.whatsappclone.feature.settings.ui.NotificationSettingsScreen
import com.whatsappclone.feature.settings.ui.PrivacySettingsScreen
import com.whatsappclone.feature.settings.ui.SettingsScreen

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

    val navDuration = 300
    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(navDuration)
            ) + fadeIn(tween(navDuration))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(navDuration)
            ) + fadeOut(tween(navDuration / 2))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(navDuration)
            ) + fadeIn(tween(navDuration))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(navDuration)
            ) + fadeOut(tween(navDuration / 2))
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
                }
            )
        }

        // ── Chat Detail ──────────────────────────────────────────────────────
        composable(
            route = AppRoute.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) {
            ChatDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onViewContact = {
                    // Phase 2: navigate to contact/group info
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
                    navController.navigate(AppRoute.NewGroup.route)
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
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            PlaceholderScreen(label = "Contact Info\nuserId: $userId")
        }

        // ── New Group ────────────────────────────────────────────────────────
        composable(AppRoute.NewGroup.route) {
            PlaceholderScreen(
                label = "New Group",
                onNavigate = {
                    navController.navigate(AppRoute.GroupSetup.route)
                }
            )
        }

        // ── Group Setup ──────────────────────────────────────────────────────
        composable(AppRoute.GroupSetup.route) {
            PlaceholderScreen(
                label = "Group Setup",
                onNavigate = {
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(AppRoute.Main.route) { inclusive = true }
                    }
                }
            )
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

        // ── Profile Edit ─────────────────────────────────────────────────────
        composable(AppRoute.ProfileEdit.route) {
            ProfileEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Privacy Settings ─────────────────────────────────────────────────
        composable(AppRoute.PrivacySettings.route) {
            PrivacySettingsScreen(
                onNavigateToBlockedContacts = {
                    // Placeholder: blocked contacts screen
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
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

        // ── Forward Picker ───────────────────────────────────────────────────
        composable(
            route = AppRoute.ForwardPicker.route,
            arguments = listOf(
                navArgument("messageId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
            PlaceholderScreen(label = "Forward Picker\nmessageId: $messageId")
        }
    }
}

/**
 * Temporary placeholder screen for features not yet implemented.
 * Will be replaced as each feature is built out in subsequent phases.
 */
@Composable
private fun PlaceholderScreen(
    label: String,
    onNavigate: (() -> Unit)? = null
) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }

    if (onNavigate != null) {
        LaunchedEffect(Unit) {
            // Placeholder screens don't auto-navigate; real screens handle
            // navigation via user actions and ViewModel events.
        }
    }
}
