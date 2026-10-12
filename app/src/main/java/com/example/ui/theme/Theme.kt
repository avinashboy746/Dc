package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DiscordDarkColorScheme = darkColorScheme(
    primary = DiscordBlurple,
    onPrimary = Color.White,
    primaryContainer = DiscordSurface,
    onPrimaryContainer = DiscordTextPrimary,
    secondary = DiscordGreen,
    onSecondary = Color.Black,
    background = DiscordDark,
    onBackground = DiscordTextPrimary,
    surface = DiscordSurface,
    onSurface = DiscordTextPrimary,
    surfaceVariant = DiscordChatBg,
    onSurfaceVariant = DiscordTextMuted,
    error = DiscordRed,
    onError = Color.White
)

private val DiscordLightColorScheme = lightColorScheme(
    primary = DiscordBlurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E5E8),
    onPrimaryContainer = Color(0xFF060607),
    secondary = DiscordGreen,
    onSecondary = Color.Black,
    background = Color(0xFFF2F3F5),
    onBackground = Color(0xFF060607),
    surface = Color.White,
    onSurface = Color(0xFF060607),
    surfaceVariant = Color(0xFFEBEDEF),
    onSurfaceVariant = Color(0xFF4E5058),
    error = DiscordRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DiscordDarkColorScheme else DiscordLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
