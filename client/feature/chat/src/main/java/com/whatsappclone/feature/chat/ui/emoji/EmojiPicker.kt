package com.whatsappclone.feature.chat.ui.emoji

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val RECENT_EMOJI_PREFS = "recent_emojis"
private const val RECENT_EMOJI_KEY = "recent_list"
private const val MAX_RECENT = 30
private const val GRID_COLUMNS = 6

@Composable
fun EmojiPicker(
    visible: Boolean,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            expandFrom = Alignment.Top
        ) + fadeIn(),
        exit = shrinkVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            shrinkTowards = Alignment.Top
        ) + fadeOut(),
        modifier = modifier
    ) {
        EmojiPickerContent(
            onEmojiSelected = onEmojiSelected
        )
    }
}

@Composable
private fun EmojiPickerContent(
    onEmojiSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories = remember { EmojiData.categories }
    val gridState = rememberLazyGridState()

    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var recentEmojis by remember { mutableStateOf(loadRecentEmojis(context)) }

    val isSearchActive = searchQuery.isNotEmpty()

    val filteredEmojis = remember(searchQuery) {
        if (searchQuery.isEmpty()) emptyList()
        else EmojiData.allEmojis.filter { emoji ->
            emoji.contains(searchQuery, ignoreCase = true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChanged = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Category tab bar (hidden during search)
            if (!isSearchActive) {
                CategoryTabBar(
                    categories = categories,
                    selectedIndex = selectedCategoryIndex,
                    hasRecents = recentEmojis.isNotEmpty(),
                    onCategorySelected = { index ->
                        selectedCategoryIndex = index
                        scope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    }
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )

            // Emoji grid
            if (isSearchActive) {
                if (filteredEmojis.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No emojis found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        items(filteredEmojis) { emoji ->
                            EmojiCell(
                                emoji = emoji,
                                onClick = {
                                    onEmojiSelected(emoji)
                                    recentEmojis = addRecentEmoji(context, emoji, recentEmojis)
                                }
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Recently used section
                    if (selectedCategoryIndex == 0 && recentEmojis.isNotEmpty()) {
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            SectionHeader(title = "Recently used")
                        }
                        items(recentEmojis) { emoji ->
                            EmojiCell(
                                emoji = emoji,
                                onClick = {
                                    onEmojiSelected(emoji)
                                    recentEmojis = addRecentEmoji(context, emoji, recentEmojis)
                                }
                            )
                        }
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Category emojis
                    val currentCategory = categories.getOrNull(selectedCategoryIndex)
                    if (currentCategory != null) {
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            SectionHeader(title = currentCategory.name)
                        }
                        items(currentCategory.emojis) { emoji ->
                            EmojiCell(
                                emoji = emoji,
                                onClick = {
                                    onEmojiSelected(emoji)
                                    recentEmojis = addRecentEmoji(context, emoji, recentEmojis)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search emoji",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChanged("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTabBar(
    categories: List<EmojiCategory>,
    selectedIndex: Int,
    hasRecents: Boolean,
    onCategorySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(categories) { index, category ->
            CategoryTab(
                emoji = category.icon,
                isSelected = index == selectedIndex,
                onClick = { onCategorySelected(index) }
            )
        }
    }
}

@Composable
private fun CategoryTab(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(16.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        fontSize = 12.sp
    )
}

@Composable
private fun EmojiCell(
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

// ── Persistence for recent emojis ─────────────────────────────────────────────

private fun loadRecentEmojis(context: Context): List<String> {
    val prefs = context.getSharedPreferences(RECENT_EMOJI_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(RECENT_EMOJI_KEY, null) ?: return emptyList()
    return raw.split(",").filter { it.isNotEmpty() }.take(MAX_RECENT)
}

private fun addRecentEmoji(
    context: Context,
    emoji: String,
    current: List<String>
): List<String> {
    val updated = listOf(emoji) + current.filter { it != emoji }
    val trimmed = updated.take(MAX_RECENT)
    context.getSharedPreferences(RECENT_EMOJI_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(RECENT_EMOJI_KEY, trimmed.joinToString(","))
        .apply()
    return trimmed
}
