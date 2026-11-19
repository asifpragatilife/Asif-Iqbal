package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Core Brand Palette — Obsidian Vault meets Pro Toolkit
val Navy950 = Color(0xFF0A1220)
val Navy900 = Color(0xFF0F1B2D)
val Navy850 = Color(0xFF132238)
val Navy800 = Color(0xFF192B47)
val Navy700 = Color(0xFF233B61)

val ElectricBlue = Color(0xFF2F7BFF)
val ElectricBlueSoft = Color(0xFF5A9BFF)
val CyanGlow = Color(0xFF22D3EE)
val CyanSoft = Color(0xFF67E8F9)

// Folder & Category Accents
val FolderBlue = Color(0xFF2F7BFF)
val FolderGreen = Color(0xFF34D399)
val FolderOrange = Color(0xFFFB923C)
val FolderPurple = Color(0xFFA78BFA)

// Semantic Status
val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val CrimsonDanger = Color(0xFFEF4444)

// Light Theme Tokens
val LightCanvas = Color(0xFFF3F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceAlt = Color(0xFFE8EEF8)
val LightInk = Color(0xFF0B1526)
val LightInkMuted = Color(0xFF53637D)

@Immutable
data class AsifArafExtraColors(
    val isDark: Boolean,
    val gradientTop: Color,
    val gradientBottom: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val accentCyan: Color,
    val navBarBackground: Color,
    val badgeBg: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

val DarkExtraColors = AsifArafExtraColors(
    isDark = true,
    gradientTop = Navy950,
    gradientBottom = Navy900,
    glassSurface = Color(0xB8132238),
    glassSurfaceElevated = Color(0xDC192B47),
    glassBorder = Color(0x26FFFFFF),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF7C90B0),
    accentBlue = ElectricBlue,
    accentCyan = CyanGlow,
    navBarBackground = Color(0xEB0B1526),
    badgeBg = Color(0x332F7BFF),
    success = FolderGreen,
    warning = FolderOrange,
    danger = CrimsonDanger,
)

val LightExtraColors = AsifArafExtraColors(
    isDark = false,
    gradientTop = LightCanvas,
    gradientBottom = Color(0xFFE9F0FA),
    glassSurface = Color(0xEBFFFFFF),
    glassSurfaceElevated = Color(0xFFFFFFFF),
    glassBorder = Color(0x1F0F1B2D),
    textPrimary = LightInk,
    textSecondary = Color(0xFF334155),
    textMuted = LightInkMuted,
    accentBlue = ElectricBlue,
    accentCyan = Color(0xFF0891B2),
    navBarBackground = Color(0xF5FFFFFF),
    badgeBg = Color(0x1A2F7BFF),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    danger = CrimsonDanger,
)

fun folderColorForKey(key: String): Color = when (key.lowercase()) {
    "green" -> FolderGreen
    "orange" -> FolderOrange
    "purple" -> FolderPurple
    else -> FolderBlue
}
