package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ChecklistItem
import com.example.data.NoteColor
import com.example.data.NotesRepository
import com.example.data.TreeNote
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    editingNote: TreeNote?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        content: String,
        color: NoteColor,
        pinnedToTree: Boolean,
        checklistMode: Boolean,
        checklistItems: List<ChecklistItem>,
        treeTier: Long
    ) -> Unit,
    onDelete: (String) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(editingNote) { mutableStateOf(editingNote?.title.orEmpty()) }
    var content by remember(editingNote) { mutableStateOf(editingNote?.content.orEmpty()) }
    var selectedColor by remember(editingNote) {
        mutableStateOf(editingNote?.noteColor ?: NoteColor.CREAM)
    }
    var pinnedToTree by remember(editingNote) {
        mutableStateOf(editingNote?.pinnedToTree ?: true)
    }
    var checklistMode by remember(editingNote) {
        mutableStateOf(editingNote?.checklistMode ?: false)
    }
    var treeTier by remember(editingNote) {
        mutableLongStateOf(editingNote?.treeTier ?: 0L)
    }
    val itemsState = remember(editingNote) {
        mutableStateListOf<ChecklistItem>().apply {
            val existing = editingNote?.checklistItems.orEmpty()
            if (existing.isNotEmpty()) {
                addAll(existing)
            } else {
                add(ChecklistItem(itemId = UUID.randomUUID().toString().take(12), text = "", checked = false))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = selectedColor.containerColor(isDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar: Title + Delete + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingNote == null) "新建樱花便利贴" else "编辑便利贴",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    if (editingNote != null) {
                        IconButton(
                            onClick = { onDelete(editingNote.id) },
                            modifier = Modifier.testTag("delete_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "删除便利贴",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭编辑器"
                        )
                    }
                }
            }

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= NotesRepository.MAX_TITLE_LENGTH) title = it },
                label = { Text("标题") },
                placeholder = { Text("例如：灵感速记、周末采购清单...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            // Mode toggle: Text vs Checklist
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !checklistMode,
                    onClick = { checklistMode = false },
                    leadingIcon = {
                        Icon(Icons.Rounded.Notes, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("文本便签") },
                    modifier = Modifier.testTag("mode_text_chip")
                )
                FilterChip(
                    selected = checklistMode,
                    onClick = { checklistMode = true },
                    leadingIcon = {
                        Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("待办清单") },
                    modifier = Modifier.testTag("mode_checklist_chip")
                )
            }

            if (!checklistMode) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= NotesRepository.MAX_CONTENT_LENGTH) content = it },
                    label = { Text("记事内容") },
                    placeholder = { Text("写下想法、备忘或今日灵感...") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsState.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    itemsState[index] = item.copy(checked = !item.checked)
                                }
                            ) {
                                Icon(
                                    imageVector = if (item.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = "切换完成状态"
                                )
                            }
                            OutlinedTextField(
                                value = item.text,
                                onValueChange = { text ->
                                    if (text.length <= 200) {
                                        itemsState[index] = item.copy(text = text)
                                    }
                                },
                                placeholder = { Text("第 ${index + 1} 项待办") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checklist_item_input_$index")
                            )
                            if (itemsState.size > 1) {
                                IconButton(
                                    onClick = { itemsState.removeAt(index) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "移除此项"
                                    )
                                }
                            }
                        }
                    }

                    if (itemsState.size < NotesRepository.MAX_CHECKLIST_ITEMS) {
                        TextButton(
                            onClick = {
                                itemsState.add(
                                    ChecklistItem(
                                        itemId = UUID.randomUUID().toString().take(12),
                                        text = "",
                                        checked = false
                                    )
                                )
                            },
                            modifier = Modifier.testTag("add_checklist_item_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加待办项")
                        }
                    }
                }
            }

            // Color Ornament Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "便利贴纸色彩",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(NoteColor.entries) { noteColor ->
                        val selected = selectedColor == noteColor
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(noteColor.containerColor(isDark))
                                .border(
                                    width = if (selected) 2.5.dp else 1.5.dp,
                                    color = if (selected) noteColor.ornamentColor else noteColor.borderColor(isDark),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = noteColor }
                                .testTag("color_option_${noteColor.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "已选 ${noteColor.labelZh}",
                                    tint = noteColor.ornamentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(noteColor.ornamentColor)
                                )
                            }
                        }
                    }
                }
            }

            // Pin to Christmas Tree + Tier selection
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "置顶到 2×2 樱花小组件",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "开启后会在桌面 2×2 花瓣小组件中优先展示",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pinnedToTree,
                            onCheckedChange = { pinnedToTree = it },
                            modifier = Modifier.testTag("pin_to_tree_switch")
                        )
                    }

                    if (pinnedToTree) {
                        val tiers = listOf(
                            0L to "自动",
                            1L to "顺位 1",
                            2L to "顺位 2",
                            3L to "顺位 3",
                            4L to "顺位 4"
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tiers) { (tierVal, label) ->
                                FilterChip(
                                    selected = treeTier == tierVal,
                                    onClick = { treeTier = tierVal },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        editingNote?.id,
                        title,
                        content,
                        selectedColor,
                        pinnedToTree,
                        checklistMode,
                        itemsState.toList(),
                        treeTier
                    )
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_note_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在同步...")
                } else {
                    Text(
                        text = "保存并更新小组件",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
