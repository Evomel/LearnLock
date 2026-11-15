package com.learnlock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LlColors.MintDeep,
    onPrimary = LlColors.TextOnAccent,
    primaryContainer = LlColors.MintBright.copy(alpha = 0.25f),
    onPrimaryContainer = LlColors.Ink,
    secondary = LlColors.Coral,
    onSecondary = LlColors.TextOnAccent,
    secondaryContainer = LlColors.CoralSoft.copy(alpha = 0.35f),
    onSecondaryContainer = LlColors.Ink,
    tertiary = LlColors.Sky,
    background = LlColors.Mist,
    onBackground = LlColors.TextPrimary,
    surface = LlColors.Surface,
    onSurface = LlColors.TextPrimary,
    surfaceVariant = LlColors.MistDeep,
    onSurfaceVariant = LlColors.TextSecondary,
    outline = LlColors.MistDeep
)

private val DarkColors = darkColorScheme(
    primary = LlColors.MintBright,
    onPrimary = LlColors.Ink,
    primaryContainer = LlColors.MintDeep,
    onPrimaryContainer = LlColors.Mist,
    secondary = LlColors.CoralSoft,
    onSecondary = LlColors.Ink,
    tertiary = LlColors.Sky,
    background = LlColors.Ink,
    onBackground = LlColors.Mist,
    surface = LlColors.InkSoft,
    onSurface = LlColors.Mist,
    surfaceVariant = LlColors.InkSoft,
    onSurfaceVariant = LlColors.MistDeep,
    outline = LlColors.TextSecondary
)

@Composable
fun LearnLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LlTypography,
        content = content
    )
}
