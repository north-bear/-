package com.example.data

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class NoteColor(
    val labelZh: String,
    val lightBg: Color,
    val lightBorder: Color,
    val darkBg: Color,
    val darkBorder: Color,
    val ornamentColor: Color
) {
    CREAM(
        labelZh = "暖雪白",
        lightBg = Color(0xFFFDF6E7),
        lightBorder = Color(0xFFDFD2B7),
        darkBg = Color(0xFF2B2820),
        darkBorder = Color(0xFF584E3A),
        ornamentColor = Color(0xFFE0B451)
    ),
    PINE(
        labelZh = "青雾绿",
        lightBg = Color(0xFFE3F4EA),
        lightBorder = Color(0xFFA0CCB2),
        darkBg = Color(0xFF1A3326),
        darkBorder = Color(0xFF396950),
        ornamentColor = Color(0xFF288255)
    ),
    CRANBERRY(
        labelZh = "落樱粉",
        lightBg = Color(0xFFFEE8EC),
        lightBorder = Color(0xFFEBACB9),
        darkBg = Color(0xFF392026),
        darkBorder = Color(0xFF763C49),
        ornamentColor = Color(0xFFD85B79)
    ),
    AMBER(
        labelZh = "暖阳金",
        lightBg = Color(0xFFFFF2D4),
        lightBorder = Color(0xFFE6C06E),
        darkBg = Color(0xFF372C16),
        darkBorder = Color(0xFF705827),
        ornamentColor = Color(0xFFE69D1E)
    ),
    FROST(
        labelZh = "晴空蓝",
        lightBg = Color(0xFFE6F2FC),
        lightBorder = Color(0xFFA6CBEB),
        darkBg = Color(0xFF1A2B38),
        darkBorder = Color(0xFF355873),
        ornamentColor = Color(0xFF3C88C7)
    ),
    BERRY(
        labelZh = "紫丁香",
        lightBg = Color(0xFFF4E8FA),
        lightBorder = Color(0xFFCCAEE0),
        darkBg = Color(0xFF2E2036),
        darkBorder = Color(0xFF5D3E6F),
        ornamentColor = Color(0xFF9357B8)
    );

    fun containerColor(isDark: Boolean): Color = if (isDark) darkBg else lightBg
    fun borderColor(isDark: Boolean): Color = if (isDark) darkBorder else lightBorder

    companion object {
        fun safeFromKey(raw: String?): NoteColor {
            return entries.firstOrNull { it.name == raw } ?: CREAM
        }
    }
}

enum class BlossomShape(val labelZh: String) {
    M3_BLOSSOM("MD3 七瓣曲奇"),
    FIVE_PETAL_SAKURA("五瓣樱花");

    companion object {
        fun safeFromName(raw: String?): BlossomShape {
            return entries.firstOrNull { it.name == raw } ?: M3_BLOSSOM
        }
    }
}

enum class BlossomPalette(
    val labelZh: String,
    val lightFillHex: Long,
    val lightBorderHex: Long,
    val lightTextPrimaryHex: Long,
    val lightTextSecondaryHex: Long,
    val lightBadgeBgHex: Long,
    val lightBadgeFgHex: Long,
    val darkFillHex: Long,
    val darkBorderHex: Long,
    val darkTextPrimaryHex: Long,
    val darkTextSecondaryHex: Long,
    val darkBadgeBgHex: Long,
    val darkBadgeFgHex: Long
) {
    SYSTEM_MD3(
        labelZh = "跟随系统 MD3",
        lightFillHex = 0xFFCCEFE7,
        lightBorderHex = 0x33006B5E,
        lightTextPrimaryHex = 0xFF06201C,
        lightTextSecondaryHex = 0xCC1C3A35,
        lightBadgeBgHex = 0xFF006B5E,
        lightBadgeFgHex = 0xFFFFFFFF,
        darkFillHex = 0xFF0F3A35,
        darkBorderHex = 0x388EE0D2,
        darkTextPrimaryHex = 0xFFE2F6F1,
        darkTextSecondaryHex = 0xFFB8DAD2,
        darkBadgeBgHex = 0xFF3EC2AE,
        darkBadgeFgHex = 0xFF003730
    ),
    MATCH_NOTE(
        labelZh = "随便签变色",
        lightFillHex = 0xFFCCEFE7,
        lightBorderHex = 0xFF9AC9BD,
        lightTextPrimaryHex = 0xFF14221D,
        lightTextSecondaryHex = 0xCC35463F,
        lightBadgeBgHex = 0xFF006B5E,
        lightBadgeFgHex = 0xFFFFFFFF,
        darkFillHex = 0xFF1A3326,
        darkBorderHex = 0xFF396950,
        darkTextPrimaryHex = 0xFFE8F5EE,
        darkTextSecondaryHex = 0xCCB8D6C6,
        darkBadgeBgHex = 0xFF64D6A3,
        darkBadgeFgHex = 0xFF08291A
    ),
    SAGE_TEAL(
        labelZh = "森绿青瓷",
        lightFillHex = 0xFFCDEEE5,
        lightBorderHex = 0x44006B5E,
        lightTextPrimaryHex = 0xFF05201C,
        lightTextSecondaryHex = 0xCC1F3C37,
        lightBadgeBgHex = 0xFF006B5E,
        lightBadgeFgHex = 0xFFFFFFFF,
        darkFillHex = 0xFF0E3934,
        darkBorderHex = 0x407ED6C6,
        darkTextPrimaryHex = 0xFFE2F6F1,
        darkTextSecondaryHex = 0xFFB5D8D0,
        darkBadgeBgHex = 0xFF3EC2AE,
        darkBadgeFgHex = 0xFF003730
    ),
    SAKURA_BLUSH(
        labelZh = "樱花云霞",
        lightFillHex = 0xFFFCE2E9,
        lightBorderHex = 0x44B5385D,
        lightTextPrimaryHex = 0xFF3B1120,
        lightTextSecondaryHex = 0xCC62293C,
        lightBadgeBgHex = 0xFFB5385D,
        lightBadgeFgHex = 0xFFFFFFFF,
        darkFillHex = 0xFF4B2130,
        darkBorderHex = 0x44F7B1C6,
        darkTextPrimaryHex = 0xFFFFECEF,
        darkTextSecondaryHex = 0xCCE9C2CE,
        darkBadgeBgHex = 0xFFF49BB4,
        darkBadgeFgHex = 0xFF471022
    ),
    KEEP_CREAM(
        labelZh = "暖纸米黄",
        lightFillHex = 0xFFFDF0D1,
        lightBorderHex = 0x448C6200,
        lightTextPrimaryHex = 0xFF281D07,
        lightTextSecondaryHex = 0xCC55431C,
        lightBadgeBgHex = 0xFF946600,
        lightBadgeFgHex = 0xFFFFFFFF,
        darkFillHex = 0xFF3B3018,
        darkBorderHex = 0x44E9C97B,
        darkTextPrimaryHex = 0xFFFAF0D8,
        darkTextSecondaryHex = 0xCCD9C89F,
        darkBadgeBgHex = 0xFFE8C268,
        darkBadgeFgHex = 0xFF322300
    );

    fun fillHex(isDark: Boolean): Long = if (isDark) darkFillHex else lightFillHex
    fun borderHex(isDark: Boolean): Long = if (isDark) darkBorderHex else lightBorderHex
    fun textPrimaryHex(isDark: Boolean): Long = if (isDark) darkTextPrimaryHex else lightTextPrimaryHex
    fun textSecondaryHex(isDark: Boolean): Long = if (isDark) darkTextSecondaryHex else lightTextSecondaryHex
    fun badgeBgHex(isDark: Boolean): Long = if (isDark) darkBadgeBgHex else lightBadgeBgHex
    fun badgeFgHex(isDark: Boolean): Long = if (isDark) darkBadgeFgHex else lightBadgeFgHex

    companion object {
        fun safeFromName(raw: String?): BlossomPalette {
            return entries.firstOrNull { it.name == raw } ?: SYSTEM_MD3
        }
    }
}

@Keep
@IgnoreExtraProperties
data class ChecklistItem(
    val itemId: String = "",
    val text: String = "",
    val checked: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class TreeNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val colorKey: String = NoteColor.CREAM.name,
    val pinnedToTree: Boolean = false,
    val checklistMode: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val treeTier: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val noteColor: NoteColor
        get() = NoteColor.safeFromKey(colorKey)

    fun widgetTitle(): String {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isNotEmpty()) return trimmedTitle
        if (checklistMode && checklistItems.isNotEmpty()) {
            return checklistItems.first().text.trim().ifEmpty { "待办清单" }
        }
        return content.trim().lineSequence().firstOrNull()?.take(24) ?: "未命名便利贴"
    }

    fun widgetSnippet(): String {
        return if (checklistMode) {
            val total = checklistItems.size
            val done = checklistItems.count { it.checked }
            val nextText = checklistItems.firstOrNull { !it.checked }?.text?.trim().orEmpty()
            if (total == 0) "空清单"
            else if (nextText.isNotEmpty()) "✓ $done/$total · $nextText"
            else "✓ 全部完成 ($done/$total)"
        } else {
            content.trim().replace("\n", " ").ifEmpty { "轻点编辑内容" }
        }
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
