package com.tiborhreus.photoslidesshow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Tmavá paleta s kontrastnými okrajmi tlačidiel. */
object AppColors {
    val Background = Color(0xFF0B0C10)
    val Surface = Color(0xFF15171D)
    val SurfaceHigh = Color(0xFF1E2129)
    val Border = Color(0xFFE4E6EB)      // svetlý okraj bežných tlačidiel
    val BorderDim = Color(0xFF3A3F4B)   // jemný okraj kariet
    val Accent = Color(0xFFFFB627)      // jantárová – hlavné akcie a výbery
    val OnAccent = Color(0xFF111111)
    val Cyan = Color(0xFF4DD8FF)
    val Text = Color(0xFFF4F5F7)
    val TextMuted = Color(0xFFA3A9B5)
    val Danger = Color(0xFFFF5C5C)
}

private val DarkScheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.OnAccent,
    secondary = AppColors.Cyan,
    onSecondary = AppColors.OnAccent,
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.SurfaceHigh,
    onSurfaceVariant = AppColors.TextMuted,
    surfaceContainer = AppColors.Surface,
    surfaceContainerHigh = AppColors.SurfaceHigh,
    surfaceContainerHighest = AppColors.SurfaceHigh,
    outline = AppColors.Border,
    outlineVariant = AppColors.BorderDim,
    error = AppColors.Danger,
)

@Composable
fun PhotoSlidesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography(),
        content = content,
    )
}
