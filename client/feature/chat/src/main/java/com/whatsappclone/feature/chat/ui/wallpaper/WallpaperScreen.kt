package com.whatsappclone.feature.chat.ui.wallpaper

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val wallpaperColors = listOf(
    0xFFE8F5E9L to "Mint Green",
    0xFFE3F2FDL to "Light Blue",
    0xFFFFF3E0L to "Peach",
    0xFFF3E5F5L to "Lavender",
    0xFFFFFDE7L to "Lemon",
    0xFFEFEBE9L to "Warm Gray",
    0xFFE0F2F1L to "Teal Light",
    0xFFFCE4ECL to "Rose",
    0xFFECEFF1L to "Blue Gray",
    0xFFF1F8E9L to "Lime Light",
    0xFFE8EAF6L to "Indigo Light",
    0xFFFFF8E1L to "Amber Light",
    0xFFEDE7F6L to "Deep Purple Light",
    0xFFE0F7FAL to "Cyan Light",
    0xFFF9FBE7L to "Yellow Green",
    0xFFFBE9E7L to "Deep Orange Light"
)

private const val PREFS_NAME = "wallpaper_prefs"

fun getWallpaperColor(context: Context, chatId: String): Long? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val chatSpecific = prefs.getLong("wallpaper_$chatId", -1L)
    if (chatSpecific != -1L) return chatSpecific
    val defaultVal = prefs.getLong("wallpaper_default", -1L)
    if (defaultVal != -1L) return defaultVal
    return null
}

private fun saveWallpaper(context: Context, chatId: String, colorLong: Long?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (colorLong == null) {
        prefs.edit().remove("wallpaper_$chatId").apply()
    } else {
        prefs.edit().putLong("wallpaper_$chatId", colorLong).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    chatId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedColor by remember {
        mutableStateOf(getWallpaperColor(context, chatId))
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chat wallpaper",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Solid colours",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (selectedColor == null) Modifier.border(
                                    3.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .clickable {
                                selectedColor = null
                                saveWallpaper(context, chatId, null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatColorReset,
                            contentDescription = "Default",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(wallpaperColors) { (colorLong, name) ->
                    val isSelected = selectedColor == colorLong
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(colorLong))
                            .then(
                                if (isSelected) Modifier.border(
                                    3.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .clickable {
                                selectedColor = colorLong
                                saveWallpaper(context, chatId, colorLong)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        selectedColor?.let { Color(it) }
                            ?: MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chat preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}
