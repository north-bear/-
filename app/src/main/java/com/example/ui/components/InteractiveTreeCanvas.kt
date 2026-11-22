package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BlossomPalette
import com.example.data.BlossomShape
import com.example.data.TreeNote
import com.example.data.WidgetConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveTreeCanvas(
    notes: List<TreeNote>,
    config: WidgetConfig,
    onStarQuickAdd: () -> Unit,
    onNoteClick: (TreeNote) -> Unit,
    onTogglePin: (TreeNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    var activeIndex by remember(notes.size) { mutableIntStateOf(0) }
    val safeIndex = if (notes.isEmpty()) 0 else activeIndex.coerceIn(0, notes.lastIndex)
    val activeNote = notes.getOrNull(safeIndex)

    val stageGradient = if (isDark) {
        listOf(Color(0xFF0E2120), Color(0xFF1A3A36), Color(0xFF0B1918))
    } else {
        listOf(Color(0xFFD9EAE6), Color(0xFFE9F3F0), Color(0xFFCDE2DC))
    }
    val stageCaptionColor = if (isDark) Color(0xFFD6ECE6) else Color(0xFF193832)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors = stageGradient))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle wallpaper ambient glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x385EA394) else Color(0x55FFFFFF),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.22f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.78f, size.height * 0.22f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "桌面 2×2 实时预览 · 系统${if (isDark) "深色" else "浅色"}",
                style = MaterialTheme.typography.labelMedium,
                color = stageCaptionColor.copy(alpha = 0.85f)
            )

            // 2x2 MD3 Cookie / Sakura Widget Container
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                val colors = resolvePreviewColors(
                    palette = config.palette,
                    note = activeNote,
                    isDark = isDark,
                    colorScheme = colorScheme
                )

                // MD3 7-lobed Cookie / 5-petal Sakura Shape Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (activeNote != null) onNoteClick(activeNote) else onStarQuickAdd()
                        }
                        .testTag("blossom_widget_body")
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.475f
                    val path = buildComposeBlossomPath(cx, cy, r, config.shape)
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(colors.fill, colors.fill.copy(alpha = 0.94f))
                        )
                    )
                    drawPath(
                        path = path,
                        color = colors.border,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    if (config.showPetals) {
                        drawCircle(
                            color = Color.White.copy(alpha = if (isDark) 0.12f else 0.22f),
                            radius = 3.5.dp.toPx(),
                            center = Offset(cx - r * 0.56f, cy - r * 0.54f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = if (isDark) 0.10f else 0.18f),
                            radius = 2.8.dp.toPx(),
                            center = Offset(cx + r * 0.60f, cy + r * 0.48f)
                        )
                    }
                }

                // Interior content layered cleanly inside the MD3 Cookie
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 14.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Solid circular MD3 badge nestled inside the top lobe (matching screenshot)
                    Surface(
                        shape = CircleShape,
                        color = colors.badgeBg,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(onClick = onStarQuickAdd)
                            .testTag("tree_star_add_button")
                            .semantics { contentDescription = "快速新建便利贴" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = colors.badgeFg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (activeNote == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "轻点 + 新建",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "置顶便签将呈现在 MD3 花瓣中",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeNote.widgetTitle(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = { onTogglePin(activeNote) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = if (activeNote.pinnedToTree) "取消置顶" else "置顶到小组件",
                                        tint = if (activeNote.pinnedToTree) colors.primaryText else colors.secondaryText.copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (activeNote.checklistMode && activeNote.checklistItems.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    activeNote.checklistItems.take(2).forEach { item ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (item.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                tint = colors.secondaryText,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = item.text,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.secondaryText,
                                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = activeNote.widgetSnippet(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.secondaryText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Bottom Pager Pill
                    if (notes.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primaryText.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activeIndex = (safeIndex - 1 + notes.size) % notes.size
                                    },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .testTag("preview_prev_note")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "上一条",
                                        tint = colors.primaryText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "${safeIndex + 1}/${notes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primaryText
                                )
                                IconButton(
                                    onClick = {
                                        activeIndex = (safeIndex + 1) % notes.size
                                    },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .testTag("preview_next_note")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "下一条",
                                        tint = colors.primaryText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SakuraBlossomHeroIllustration(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val bgColors = if (isDark) {
        listOf(Color(0xFF0E2120), Color(0xFF1A3A36), Color(0xFF0C1B1A))
    } else {
        listOf(Color(0xFFD6EAE4), Color(0xFFE7F4F0), Color(0xFFCDE3DC))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors = bgColors)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val leftPath = buildComposeBlossomPath(w * 0.25f, h * 0.56f, h * 0.22f, BlossomShape.FIVE_PETAL_SAKURA)
            drawPath(leftPath, color = scheme.tertiaryContainer.copy(alpha = 0.55f))

            val rightPath = buildComposeBlossomPath(w * 0.76f, h * 0.44f, h * 0.19f, BlossomShape.FIVE_PETAL_SAKURA)
            drawPath(rightPath, color = scheme.secondaryContainer.copy(alpha = 0.55f))

            // Hero 2x2 MD3 7-lobed Cookie in center
            val centerPath = buildComposeBlossomPath(w * 0.5f, h * 0.52f, h * 0.40f, BlossomShape.M3_BLOSSOM)
            drawPath(
                path = centerPath,
                color = scheme.primaryContainer
            )
            drawPath(
                path = centerPath,
                color = scheme.outline.copy(alpha = 0.28f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "MD3 樱花记事",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer
            )
            Text(
                text = "2×2 花瓣桌面微件 · 亮暗随心",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

private data class PreviewColors(
    val fill: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val badgeBg: Color,
    val badgeFg: Color
)

private fun resolvePreviewColors(
    palette: BlossomPalette,
    note: TreeNote?,
    isDark: Boolean,
    colorScheme: ColorScheme
): PreviewColors {
    return when {
        palette == BlossomPalette.SYSTEM_MD3 -> {
            PreviewColors(
                fill = colorScheme.primaryContainer,
                border = colorScheme.outline.copy(alpha = 0.28f),
                primaryText = colorScheme.onPrimaryContainer,
                secondaryText = colorScheme.onPrimaryContainer.copy(alpha = 0.80f),
                badgeBg = colorScheme.primary,
                badgeFg = colorScheme.onPrimary
            )
        }
        palette == BlossomPalette.MATCH_NOTE && note != null -> {
            val nc = note.noteColor
            PreviewColors(
                fill = nc.containerColor(isDark),
                border = nc.borderColor(isDark),
                primaryText = if (isDark) Color(0xFFEFF6F2) else Color(0xFF14221D),
                secondaryText = if (isDark) Color(0xCCB8D6C6) else Color(0xCC35463F),
                badgeBg = nc.ornamentColor,
                badgeFg = Color.White
            )
        }
        else -> {
            PreviewColors(
                fill = Color(palette.fillHex(isDark)),
                border = Color(palette.borderHex(isDark)),
                primaryText = Color(palette.textPrimaryHex(isDark)),
                secondaryText = Color(palette.textSecondaryHex(isDark)),
                badgeBg = Color(palette.badgeBgHex(isDark)),
                badgeFg = Color(palette.badgeFgHex(isDark))
            )
        }
    }
}

private fun buildComposeBlossomPath(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    shape: BlossomShape
): Path {
    val path = Path()
    val steps = 200
    for (i in 0..steps) {
        val theta = (i.toDouble() / steps) * 2.0 * PI - PI / 2.0
        val a = theta + PI / 2.0
        val r = when (shape) {
            BlossomShape.M3_BLOSSOM -> {
                baseRadius * (0.932 + 0.068 * cos(7.0 * a) - 0.008 * cos(14.0 * a))
            }
            BlossomShape.FIVE_PETAL_SAKURA -> {
                baseRadius * (0.83 + 0.165 * cos(5.0 * a) - 0.036 * cos(10.0 * a))
            }
        }
        val x = cx + (r * cos(theta)).toFloat()
        val y = cy + (r * sin(theta)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
