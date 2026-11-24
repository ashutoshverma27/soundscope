package com.soundscope.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SoundScopeDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = MidnightBackground,
    primaryContainer = DeepSpaceCardElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricBlue,
    onSecondary = MidnightBackground,
    tertiary = ElectricPurple,
    onTertiary = MidnightBackground,
    background = MidnightBackground,
    onBackground = TextPrimary,
    surface = DeepSpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepSpaceCard,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun SoundScopeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoundScopeDarkColorScheme,
        typography = SoundScopeTypography,
        content = content
    )
}
