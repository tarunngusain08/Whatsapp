package com.whatsappclone.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface
)

@Immutable
data class WhatsAppColorScheme(
    val sentBubble: Color,
    val receivedBubble: Color,
    val chatBackground: Color,
    val messageMeta: Color,
    val deletedText: Color,
    val forwardedText: Color,
    val starColor: Color,
    val systemBubble: Color,
    val systemText: Color,
    val typingGreen: Color,
    val onlineGreen: Color,
    val messageHighlight: Color,
    val selectionHighlight: Color,
    val mutedIcon: Color,
    val pinnedBackground: Color,
    val durationBadge: Color,
    val downloadOverlay: Color
)

private val LightWhatsAppColors = WhatsAppColorScheme(
    sentBubble = LightSentBubble,
    receivedBubble = LightReceivedBubble,
    chatBackground = LightChatBackground,
    messageMeta = Color(0xFF667781),
    deletedText = Color(0xFF8696A0),
    forwardedText = Color(0xFF8696A0),
    starColor = Color(0xFFFFC107),
    systemBubble = Color(0xFFE2F0FD),
    systemText = Color(0xFF54656F),
    typingGreen = Color(0xFF25D366),
    onlineGreen = Color(0xFF25D366),
    messageHighlight = Color(0x3300A884),
    selectionHighlight = Color(0x2200A884),
    mutedIcon = Color(0xFFBDBDBD),
    pinnedBackground = Color(0xFFF0F4F0),
    durationBadge = Color(0xCC000000),
    downloadOverlay = Color(0x80000000)
)

private val DarkWhatsAppColors = WhatsAppColorScheme(
    sentBubble = DarkSentBubble,
    receivedBubble = DarkReceivedBubble,
    chatBackground = DarkChatBackground,
    messageMeta = Color(0xFF8696A0),
    deletedText = Color(0xFF8696A0),
    forwardedText = Color(0xFF8696A0),
    starColor = Color(0xFFFFC107),
    systemBubble = Color(0xFF182229),
    systemText = Color(0xFF8696A0),
    typingGreen = Color(0xFF25D366),
    onlineGreen = Color(0xFF25D366),
    messageHighlight = Color(0x3300A884),
    selectionHighlight = Color(0x2200A884),
    mutedIcon = Color(0xFF6B7B85),
    pinnedBackground = Color(0xFF1F2C34),
    durationBadge = Color(0xCC000000),
    downloadOverlay = Color(0x80000000)
)

val LocalWhatsAppColors = staticCompositionLocalOf {
    LightWhatsAppColors
}

object WhatsAppColors {
    val current: WhatsAppColorScheme
        @Composable
        get() = LocalWhatsAppColors.current

    val SentBubble: Color @Composable get() = current.sentBubble
    val ReceivedBubble: Color @Composable get() = current.receivedBubble
    val ChatBackground: Color @Composable get() = current.chatBackground
    val MessageMeta: Color @Composable get() = current.messageMeta
    val DeletedText: Color @Composable get() = current.deletedText
    val ForwardedText: Color @Composable get() = current.forwardedText
    val StarColor: Color @Composable get() = current.starColor
    val SystemBubble: Color @Composable get() = current.systemBubble
    val SystemText: Color @Composable get() = current.systemText
    val TypingGreen: Color @Composable get() = current.typingGreen
    val OnlineGreen: Color @Composable get() = current.onlineGreen
    val MessageHighlight: Color @Composable get() = current.messageHighlight
    val SelectionHighlight: Color @Composable get() = current.selectionHighlight
    val MutedIcon: Color @Composable get() = current.mutedIcon
    val PinnedBackground: Color @Composable get() = current.pinnedBackground
    val DurationBadge: Color @Composable get() = current.durationBadge
    val DownloadOverlay: Color @Composable get() = current.downloadOverlay
}

@Composable
fun WhatsAppCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val whatsAppColors = if (darkTheme) DarkWhatsAppColors else LightWhatsAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalWhatsAppColors provides whatsAppColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WhatsAppTypography,
            shapes = WhatsAppShapes,
            content = content
        )
    }
}
