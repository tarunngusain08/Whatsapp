package com.whatsappclone.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var globalErrorHandler: GlobalErrorHandler

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var baseUrlProvider: BaseUrlProvider

    @Inject
    lateinit var wsLifecycleManager: WsLifecycleManager

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

            WhatsAppCloneTheme {
                AppNavGraph(
                    sessionExpired = sessionExpired,
                    onSessionExpiredHandled = { sessionExpired = false },
                    deepLinkChatId = extractDeepLinkChatId(intent),
                    authRepository = authRepository,
                    baseUrlProvider = baseUrlProvider,
                    wsLifecycleManager = wsLifecycleManager,
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
