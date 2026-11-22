package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCEFE7),
    onPrimaryContainer = Color(0xFF06201C),
    secondary = CranberrySecondary,
    onSecondary = OnCranberrySecondary,
    secondaryContainer = CranberryContainer,
    onSecondaryContainer = OnCranberryContainer,
    tertiary = StarGoldTertiary,
    onTertiary = OnStarGoldTertiary,
    tertiaryContainer = StarGoldContainer,
    onTertiaryContainer = OnStarGoldContainer,
    background = IvoryBackground,
    onBackground = InkOnBackground,
    surface = PaperSurface,
    onSurface = InkOnBackground,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = MutedOnSurfaceVariant,
    outline = OutlineWarm
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3EC2AE),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF0F3A35),
    onPrimaryContainer = Color(0xFFE2F6F1),
    secondary = DarkCranberrySecondary,
    onSecondary = DarkOnCranberrySecondary,
    secondaryContainer = DarkCranberryContainer,
    onSecondaryContainer = DarkOnCranberryContainer,
    tertiary = DarkStarGoldTertiary,
    onTertiary = DarkOnStarGoldTertiary,
    tertiaryContainer = DarkStarGoldContainer,
    onTertiaryContainer = DarkOnStarGoldContainer,
    background = DarkNightBackground,
    onBackground = DarkOnBackground,
    surface = DarkNightSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val PineNoteShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PineNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PineNoteTypography,
        shapes = PineNoteShapes,
        content = content
    )
}
