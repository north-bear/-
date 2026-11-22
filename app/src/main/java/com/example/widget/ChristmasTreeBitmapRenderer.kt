package com.example.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import com.example.data.BlossomPalette
import com.example.data.BlossomShape
import com.example.data.NoteColor
import com.example.data.WidgetConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ChristmasTreeBitmapRenderer {
    // 360 x 360 ARGB_8888 (~518 KB), ideal 1:1 ratio for 2x2 Home Screen widget.
    private const val SIZE_PX = 360

    fun renderBlossomBitmap(
        config: WidgetConfig,
        activeNoteColor: NoteColor?,
        isDark: Boolean,
        customSystemColors: Triple<Int, Int, Int>? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = SIZE_PX / 2f
        val cy = SIZE_PX / 2f
        val baseRadius = SIZE_PX * 0.465f

        val blossomPath = buildBlossomPath(cx, cy, baseRadius, config.shape)

        val (topColor, bottomColor, strokeColor) = resolveColors(
            palette = config.palette,
            noteColor = activeNoteColor,
            isDark = isDark,
            customSystemColors = customSystemColors
        )

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - baseRadius,
                cx, cy + baseRadius,
                topColor,
                bottomColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(blossomPath, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = strokeColor
        }
        canvas.drawPath(blossomPath, strokePaint)

        if (config.showPetals) {
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) Color.argb(32, 255, 255, 255) else Color.argb(46, 255, 255, 255)
            }
            canvas.drawCircle(cx - baseRadius * 0.56f, cy - baseRadius * 0.54f, 5.5f, accentPaint)
            canvas.drawCircle(cx + baseRadius * 0.62f, cy + baseRadius * 0.48f, 4.5f, accentPaint)
        }

        return bitmap
    }

    private fun resolveColors(
        palette: BlossomPalette,
        noteColor: NoteColor?,
        isDark: Boolean,
        customSystemColors: Triple<Int, Int, Int>?
    ): Triple<Int, Int, Int> {
        if (palette == BlossomPalette.SYSTEM_MD3 && customSystemColors != null) {
            return customSystemColors
        }
        return if (palette == BlossomPalette.MATCH_NOTE && noteColor != null) {
            val base = noteColor.containerColor(isDark).toArgb()
            val border = noteColor.borderColor(isDark).toArgb()
            val top = Color.argb(242, Color.red(base), Color.green(base), Color.blue(base))
            val bottom = Color.argb(
                242,
                (Color.red(base) * (if (isDark) 0.90f else 0.96f)).toInt(),
                (Color.green(base) * (if (isDark) 0.90f else 0.96f)).toInt(),
                (Color.blue(base) * (if (isDark) 0.90f else 0.96f)).toInt()
            )
            Triple(top, bottom, border)
        } else {
            val c = palette.fillHex(isDark).toInt()
            val top = c
            val bottom = Color.argb(
                Color.alpha(c),
                (Color.red(c) * (if (isDark) 0.90f else 0.96f)).toInt(),
                (Color.green(c) * (if (isDark) 0.90f else 0.96f)).toInt(),
                (Color.blue(c) * (if (isDark) 0.90f else 0.96f)).toInt()
            )
            Triple(top, bottom, palette.borderHex(isDark).toInt())
        }
    }

    private fun buildBlossomPath(
        cx: Float,
        cy: Float,
        baseRadius: Float,
        shape: BlossomShape
    ): Path {
        val path = Path()
        val steps = 240
        for (i in 0..steps) {
            val theta = (i.toDouble() / steps) * 2.0 * PI - PI / 2.0
            val a = theta + PI / 2.0
            val r = when (shape) {
                // 7 smooth pillowy lobes matching Android Material 3 Expressive Cookie7Sided
                BlossomShape.M3_BLOSSOM -> {
                    baseRadius * (0.932 + 0.068 * cos(7.0 * a) - 0.008 * cos(14.0 * a))
                }
                // 5 rounded cherry blossom petals
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
}
