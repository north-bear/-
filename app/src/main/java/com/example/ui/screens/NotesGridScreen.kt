package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.NoteColor
import com.example.data.TreeNote
import com.example.data.UiState

@Composable
fun NotesGridScreen(
    notesState: UiState<List<TreeNote>>,
    searchQuery: String,
    selectedColor: NoteColor?,
    onSearchChange: (String) -> Unit,
    onColorSelect: (NoteColor?) -> Unit,
    onCreateNote: () -> Unit,
    onEditNote: (TreeNote) -> Unit,
    onTogglePin: (TreeNote) -> Unit,
    onToggleChecklistItem: (TreeNote, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Keep-style Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("搜索花瓣便利贴或待办...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "清除搜索"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Color filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedColor == null,
                        onClick = { onColorSelect(null) },
                        label = { Text("全部颜色") },
                        modifier = Modifier.testTag("filter_color_all")
                    )
                }
                items(NoteColor.entries) { noteColor ->
                    FilterChip(
                        selected = selectedColor == noteColor,
                        onClick = { onColorSelect(noteColor) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(noteColor.ornamentColor)
                            )
                        },
                        label = { Text(noteColor.labelZh) },
                        modifier = Modifier.testTag("filter_color_${noteColor.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (notesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = notesState.message,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = notesState.data.filter { note ->
                        val matchesColor = selectedColor == null || note.noteColor == selectedColor
                        val q = searchQuery.trim()
                        val matchesQuery = q.isEmpty() ||
                            note.title.contains(q, ignoreCase = true) ||
                            note.content.contains(q, ignoreCase = true) ||
                            note.checklistItems.any { it.text.contains(q, ignoreCase = true) }
                        matchesColor && matchesQuery
                    }

                    if (filtered.isEmpty()) {
                        EmptyNotesState(
                            hasSearch = searchQuery.isNotBlank() || selectedColor != null,
                            onCreateNote = onCreateNote
                        )
                    } else {
                        val pinned = filtered.filter { it.pinnedToTree }
                        val others = filtered.filter { !it.pinnedToTree }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val columns = if (maxWidth > 620.dp) 3 else 2
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(columns),
                                verticalItemSpacing = 10.dp,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 96.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (pinned.isNotEmpty()) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        SectionHeader(
                                            title = "已置顶到 2×2 小组件 (${pinned.size})",
                                            iconPinned = true
                                        )
                                    }
                                    items(pinned, key = { it.id }) { note ->
                                        KeepNoteCard(
                                            note = note,
                                            isDark = isDark,
                                            onClick = { onEditNote(note) },
                                            onTogglePin = { onTogglePin(note) },
                                            onToggleChecklistItem = { itemId ->
                                                onToggleChecklistItem(note, itemId)
                                            }
                                        )
                                    }
                                }

                                if (others.isNotEmpty()) {
                                    if (pinned.isNotEmpty()) {
                                        item(span = StaggeredGridItemSpan.FullLine) {
                                            SectionHeader(
                                                title = "其他记事 (${others.size})",
                                                iconPinned = false
                                            )
                                        }
                                    }
                                    items(others, key = { it.id }) { note ->
                                        KeepNoteCard(
                                            note = note,
                                            isDark = isDark,
                                            onClick = { onEditNote(note) },
                                            onTogglePin = { onTogglePin(note) },
                                            onToggleChecklistItem = { itemId ->
                                                onToggleChecklistItem(note, itemId)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, iconPinned: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = if (iconPinned) Icons.Rounded.LocalFlorist else Icons.Rounded.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeepNoteCard(
    note: TreeNote,
    isDark: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleChecklistItem: (String) -> Unit
) {
    val palette = note.noteColor
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.containerColor(isDark),
        border = BorderStroke(1.dp, palette.borderColor(isDark)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.ornamentColor)
                    )
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = if (note.pinnedToTree) "从 2×2 小组件取消置顶" else "置顶到 2×2 小组件",
                        tint = if (note.pinnedToTree) palette.ornamentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (note.checklistMode && note.checklistItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.checklistItems.take(5).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleChecklistItem(item.itemId) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (item.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                contentDescription = if (item.checked) "已完成" else "未完成",
                                tint = if (item.checked) palette.ornamentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.checked) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (note.checklistItems.size > 5) {
                        Text(
                            text = "+ 还有 ${note.checklistItems.size - 5} 项",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.pinnedToTree) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.ornamentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (note.treeTier in 1L..4L) "2×2 微件展示 · 第 ${note.treeTier} 顺位" else "2×2 微件展示",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ornamentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesState(
    hasSearch: Boolean,
    onCreateNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        com.example.ui.components.SakuraBlossomHeroIllustration(
            modifier = Modifier.widthIn(max = 280.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSearch) "没有找到匹配的便利贴" else "花瓣里还没有便利贴",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasSearch) "试试更换关键词或颜色筛选条件" else "点击下方按钮记下灵感、待办清单或备忘",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasSearch) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateNote,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("empty_create_note_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("新建第一条便利贴")
            }
        }
    }
}
