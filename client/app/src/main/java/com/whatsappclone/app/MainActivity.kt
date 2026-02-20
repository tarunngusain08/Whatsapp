package com.whatsappclone.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.whatsappclone.app.error.GlobalError
import com.whatsappclone.app.error.GlobalErrorHandler
import com.whatsappclone.app.lifecycle.WsLifecycleManager
import com.whatsappclone.app.navigation.AppNavGraph
import com.whatsappclone.core.network.url.BaseUrlProvider
import com.whatsappclone.core.ui.theme.WhatsAppCloneTheme
import com.whatsappclone.feature.auth.data.AuthRepository
import com.whatsappclone.feature.settings.data.PrivacyPreferencesStore
import com.whatsappclone.feature.settings.data.ThemeMode
import com.whatsappclone.feature.settings.data.ThemePreferencesStore
import com.whatsappclone.feature.settings.ui.BiometricLockScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var globalErrorHandler: GlobalErrorHandler

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var baseUrlProvider: BaseUrlProvider

    @Inject
    lateinit var wsLifecycleManager: WsLifecycleManager

    @Inject
    lateinit var callService: com.whatsappclone.feature.chat.call.CallService

    @Inject
    lateinit var themePreferencesStore: ThemePreferencesStore

    @Inject
    lateinit var privacyPreferencesStore: PrivacyPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register WsLifecycleManager with the process lifecycle so it receives
        // onStart / onStop callbacks when the entire app foregrounds / backgrounds.
        ProcessLifecycleOwner.get().lifecycle.addObserver(wsLifecycleManager)

        setContent {
            var sessionExpired by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        globalErrorHandler.errors.collect { error ->
                            when (error) {
                                is GlobalError.SessionExpired -> {
                                    sessionExpired = true
                                }
                            }
                        }
                    }
                }
            }

            val themeMode by themePreferencesStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val appLockEnabled by privacyPreferencesStore.appLockEnabled
                .collectAsState(initial = false)
            var isUnlocked by remember { mutableStateOf(false) }

            WhatsAppCloneTheme(darkTheme = darkTheme) {
                if (appLockEnabled && !isUnlocked) {
                    BiometricLockScreen(
                        onAuthenticated = { isUnlocked = true }
                    )
                } else {
                    AppNavGraph(
                        sessionExpired = sessionExpired,
                        onSessionExpiredHandled = { sessionExpired = false },
                        deepLinkChatId = extractDeepLinkChatId(intent),
                        authRepository = authRepository,
                        baseUrlProvider = baseUrlProvider,
                        wsLifecycleManager = wsLifecycleManager,
                        callService = callService,
                        isDebug = BuildConfig.DEBUG,
                        onRestartApp = {
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractDeepLinkChatId(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme == "whatsapp-clone" && uri.host == "chat") {
            return uri.lastPathSegment
        }
        return null
    }
}
