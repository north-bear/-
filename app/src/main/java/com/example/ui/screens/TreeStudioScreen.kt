package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BlossomPalette
import com.example.data.BlossomShape
import com.example.data.TreeNote
import com.example.data.UiState
import com.example.data.WidgetCacheStore
import com.example.data.WidgetConfig
import com.example.ui.components.InteractiveTreeCanvas
import com.example.widget.ChristmasTreeWidgetProvider

@Composable
fun TreeStudioScreen(
    notesState: UiState<List<TreeNote>>,
    widgetConfig: WidgetConfig,
    onCreateNote: () -> Unit,
    onEditNote: (TreeNote) -> Unit,
    onTogglePin: (TreeNote) -> Unit,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showManualGuide by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "2×2 樱花记事小组件",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "MD3 七瓣曲奇 · 跟随系统亮/暗色自动变色，点 + 快速记事",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (notesState) {
                is UiState.Loading -> {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notesState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val blossomNotes = WidgetCacheStore.selectTreeNotes(
                        notesState.data,
                        limit = widgetConfig.maxTreeNotes
                    )
                    InteractiveTreeCanvas(
                        notes = blossomNotes,
                        config = widgetConfig,
                        onStarQuickAdd = onCreateNote,
                        onNoteClick = onEditNote,
                        onTogglePin = onTogglePin
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val requested = ChristmasTreeWidgetProvider.requestPinWidget(context)
                        if (requested) {
                            Toast.makeText(context, "请在系统弹窗中确认添加 2×2 小组件", Toast.LENGTH_SHORT).show()
                        } else {
                            showManualGuide = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pin_widget_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "添加 2×2 到桌面",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onCreateNote,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("studio_add_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("新便利贴")
                }
            }

            // Shape & Palette quick customization
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFlorist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "花瓣轮廓与质感",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BlossomShape.entries.forEach { shape ->
                            FilterChip(
                                selected = widgetConfig.shape == shape,
                                onClick = { onUpdateConfig { it.copy(shape = shape) } },
                                label = { Text(shape.labelZh) },
                                modifier = Modifier.testTag("shape_chip_${shape.name.lowercase()}")
                            )
                        }
                        FilterChip(
                            selected = widgetConfig.showPetals,
                            onClick = { onUpdateConfig { it.copy(showPetals = !it.showPetals) } },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text("花瓣光点") },
                            modifier = Modifier.testTag("toggle_petals_chip")
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(BlossomPalette.entries) { palette ->
                            FilterChip(
                                selected = widgetConfig.palette == palette,
                                onClick = { onUpdateConfig { it.copy(palette = palette) } },
                                label = { Text(palette.labelZh) },
                                modifier = Modifier.testTag("palette_chip_${palette.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (showManualGuide) {
        AlertDialog(
            onDismissRequest = { showManualGuide = false },
            title = { Text("如何添加 2×2 樱花小组件") },
            text = {
                Text("当前桌面启动器未开启一键固定。请长按手机桌面空白处 -> 点击「微件 / 小组件 (Widgets)」-> 找到「樱花记事 (2×2)」并拖放到桌面即可。")
            },
            confirmButton = {
                TextButton(onClick = { showManualGuide = false }) {
                    Text("知道了")
                }
            }
        )
    }
}
