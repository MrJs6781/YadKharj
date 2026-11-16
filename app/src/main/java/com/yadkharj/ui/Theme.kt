package com.yadkharj.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.yadkharj.R

@Immutable
data class Palette(
    val bg: Color, val surface: Color, val ink: Color, val muted: Color, val line: Color,
    val income: Color, val expense: Color, val warn: Color, val accent: Color, val onAccent: Color
)

val LightPalette = Palette(
    bg = Color(0xFFF6F5F2), surface = Color(0xFFFFFFFF), ink = Color(0xFF14171A),
    muted = Color(0xFF8A8F94), line = Color(0xFFE7E5E0),
    income = Color(0xFF1F7A5C), expense = Color(0xFFD2553F), warn = Color(0xFFB7791F),
    accent = Color(0xFF14171A), onAccent = Color(0xFFF6F5F2)
)
val DarkPalette = Palette(
    bg = Color(0xFF0F1113), surface = Color(0xFF181B1E), ink = Color(0xFFEDEBE6),
    muted = Color(0xFF8B9096), line = Color(0xFF26292D),
    income = Color(0xFF4CC38A), expense = Color(0xFFFF8A72), warn = Color(0xFFE3B04B),
    accent = Color(0xFFEDEBE6), onAccent = Color(0xFF0F1113)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(primary = p.accent, background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink)
    } else {
        lightColorScheme(primary = p.accent, background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink)
    }
    CompositionLocalProvider(
        LocalPalette provides p,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(colorScheme = scheme) { content() }
    }
}
