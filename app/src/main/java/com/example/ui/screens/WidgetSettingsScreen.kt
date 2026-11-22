package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BlossomPalette
import com.example.data.BlossomShape
import com.example.data.NoteColor
import com.example.data.WidgetConfig
import com.example.widget.ChristmasTreeBitmapRenderer
import com.example.widget.ChristmasTreeWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WidgetSettingsScreen(
    userEmail: String?,
    widgetConfig: WidgetConfig,
    onUpdateConfig: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val systemSurfaceArgb = colorScheme.primaryContainer.toArgb()
    val systemBorderArgb = colorScheme.outline.copy(alpha = 0.28f).toArgb()
    val previewBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = widgetConfig,
        key2 = isDark,
        key3 = systemSurfaceArgb
    ) {
        value = withContext(Dispatchers.Default) {
            ChristmasTreeBitmapRenderer.renderBlossomBitmap(
                config = widgetConfig,
                activeNoteColor = NoteColor.PINE,
                isDark = isDark,
                customSystemColors = Triple(systemSurfaceArgb, systemSurfaceArgb, systemBorderArgb)
            ).asImageBitmap()
        }
    }

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
            Text(
                text = "2×2 小组件与账号",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Wallpaper Preview Card of actual 2x2 Home Screen Widget bitmap
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF0E2120), Color(0xFF1A3A36))
                                } else {
                                    listOf(Color(0xFFD9EAE6), Color(0xFFE9F3F0))
                                }
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val bitmap = previewBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "2×2 樱花记事桌面小组件渲染预览",
                            modifier = Modifier
                                .size(124.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.size(124.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MD3 2×2 异形桌面挂件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF102420)
                        )
                        Text(
                            text = "Material 3 曲奇轮廓随系统浅色/深色自动变色，顶部 + 号随时速记。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFFD6ECE6) else Color(0xFF2E4B45)
                        )
                        Button(
                            onClick = {
                                val ok = ChristmasTreeWidgetProvider.requestPinWidget(context)
                                Toast.makeText(
                                    context,
                                    if (ok) "请在桌面弹窗中确认添加 2×2 小组件" else "请长按桌面空白处 -> 微件 -> 樱花记事",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("settings_pin_widget_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("固定到桌面")
                        }
                    }
                }
            }

            // Customization Options
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "花瓣造型与配色",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BlossomShape.entries.forEach { shape ->
                            FilterChip(
                                selected = widgetConfig.shape == shape,
                                onClick = { onUpdateConfig { it.copy(shape = shape) } },
                                label = { Text(shape.labelZh) }
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(BlossomPalette.entries) { palette ->
                            FilterChip(
                                selected = widgetConfig.palette == palette,
                                onClick = { onUpdateConfig { it.copy(palette = palette) } },
                                label = { Text(palette.labelZh) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("花瓣光点点缀", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = widgetConfig.showPetals,
                            onCheckedChange = { checked ->
                                onUpdateConfig { it.copy(showPetals = checked) }
                            }
                        )
                    }
                }
            }

            // Google Account & Sign-Out
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "已开启云端同步",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail ?: "Google 账号已连接",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("退出 Google 登录")
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
