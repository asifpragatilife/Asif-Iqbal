package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AsifDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Navy800,
    onPrimaryContainer = CyanSoft,
    secondary = CyanGlow,
    onSecondary = Navy950,
    secondaryContainer = Navy850,
    onSecondaryContainer = Color(0xFFE0F7FA),
    tertiary = FolderPurple,
    background = Navy950,
    onBackground = Color(0xFFF8FAFC),
    surface = Navy900,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Navy850,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0x33FFFFFF),
    error = CrimsonDanger,
)

private val AsifLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FF),
    onPrimaryContainer = Navy950,
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Navy950,
    tertiary = Color(0xFF7C3AED),
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightInkMuted,
    outline = Color(0x220F1B2D),
    error = CrimsonDanger,
)

val AsifShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalAsifColors = staticCompositionLocalOf { DarkExtraColors }

object AsifTheme {
    val colors: AsifArafExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAsifColors.current
}

@Composable
fun AsifArafTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AsifDarkColorScheme else AsifLightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalAsifColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AsifArafTypography,
            shapes = AsifShapes,
            content = content,
        )
    }
}
